import { Alert, Grid, Stack } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, FuelBill } from '@/generated';
import {
  addNewFuelBillMutation,
  getMyCarOptions,
  updateFuelBillMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { hasValue, toApiDate, useFormat } from '@/utils/format';
import { derive, isSet } from '@/utils/fuel';
import { convert, toField, Units } from '@/utils/units';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import DateVatFields from './DateVatFields';
import FormActions from './FormActions';
import BillAttachments from './BillAttachments';
import useSaveBill from './useSaveBill';
import { isOfflineError, offlineQueue } from '@/offline';
import { QueuedRequest } from '@/offline/queue';

const MAX = 100_000;
/** refuels up to 5 % above the tank size are no warning */
const TANK_TOLERANCE = 1.05;

interface FuelState {
  date: Dayjs | null;
  total?: number;
  unit?: number;
  pricePerUnit?: number;
  distance?: number;
  estimateConsumption?: number;
}

const empty = (): FuelState => ({ date: dayjs() });

/**
 * Starts with total and volume only, so changing one of them calculates the
 * price again. The fields are in the units of the account.
 */
const fromBill = (bill: FuelBill, units: Units): FuelState => ({
  date: dayjs(bill.date),
  total: bill.total,
  unit: toField(bill.unit, units.volume.fromLiters, 2),
  distance: hasValue(bill.distance)
    ? toField(bill.distance, units.distance.fromKm, 1)
    : undefined,
  estimateConsumption: hasValue(bill.estimateConsumption)
    ? toField(bill.estimateConsumption, units.consumption.fromLitersPer100Km, 1)
    : undefined,
});

export default function FuelForm({
  carId,
  bill,
  queued,
}: {
  carId: number;
  /** bill to edit, a new one is created without */
  bill?: FuelBill;
  /** a new bill of the offline queue the server rejected, to correct */
  queued?: QueuedRequest;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { units } = format;
  const vat = useVatCountry(bill?.vatRate);
  const [form, setForm] = useState<FuelState>(() => {
    if (bill) return fromBill(bill, units);
    // the body is in the units of the API, like a saved bill
    if (queued) return fromBill(queued.body as unknown as FuelBill, units);
    return empty();
  });
  const [touched, setTouched] = useState(false);
  // id of the new bill: sent again (e.g. from the offline queue), it is
  // created once
  const [clientId, setClientId] = useState(
    () => queued?.id ?? crypto.randomUUID(),
  );
  const url = `/api/bill/${carId}/fuel`;

  const save = useSaveBill(
    carId,
    BillType.FUEL,
    () => {
      setForm(empty());
      setTouched(false);
      setClientId(crypto.randomUUID());
    },
    bill?.id,
  );
  const create = useMutation({
    ...addNewFuelBillMutation({ client: localClient }),
    onSuccess: async (saved) => {
      // a corrected bill of the queue is saved now
      if (queued) await offlineQueue.discard(queued.id);
      await save.onSuccess(saved);
    },
    // without an answer the device is offline: kept for later
    onError: (error, { body }) =>
      isOfflineError(error)
        ? save.queue(clientId, url, body!)
        : save.onError(error),
  });
  const update = useMutation({
    ...updateFuelBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );

  const set = (patch: Partial<FuelState>) =>
    setForm((f) => ({ ...f, ...patch }));
  // the calculation works in cent, a price per gallon is in the currency
  const priceFactor = units.fuelPrice.inCurrency ? 100 : 1;
  const derivedInCents = derive({
    ...form,
    pricePerUnit: convert(form.pricePerUnit, (price) => price * priceFactor),
  });
  const derived = {
    ...derivedInCents,
    pricePerUnit: convert(
      derivedInCents.pricePerUnit,
      (price) => price / priceFactor,
    ),
  };
  const amountCount = [form.total, form.unit, form.pricePerUnit].filter(
    isSet,
  ).length;

  const errors = {
    date: !form.date?.isValid() ? t('bill.errors.date') : undefined,
    amounts:
      amountCount < 2
        ? t('bill.errors.atLeastTwo', { context: units.volume.context })
        : undefined,
  };

  const unit = form.unit ?? derived.unit;
  const liters = convert(unit, units.volume.toLiters);
  // the nominal size is not exact, a little more often fits: catches typos like 450 l
  const overTank =
    isSet(liters) && car?.tankSize
      ? liters! > car.tankSize * TANK_TOLERANCE
      : false;
  // l/100km like the API, shown in the units of the account
  const consumption =
    isSet(liters) && isSet(form.distance)
      ? (liters! / units.distance.toKm(form.distance!)) * 100
      : undefined;

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.amounts) return;

    const body = {
      date: toApiDate(form.date),
      vatRate: vat.vatRate,
      total: form.total,
      // the API takes km, liters and cent per liter
      unit: convert(form.unit, units.volume.toLiters),
      pricePerUnit: convert(form.pricePerUnit, units.fuelPrice.toCentsPerLiter),
      distance: convert(form.distance, units.distance.toKm),
      estimateConsumption: convert(
        form.estimateConsumption,
        units.consumption.toLitersPer100Km,
      ),
    };
    if (bill?.id !== undefined) {
      update.mutate({ path: { carId, billId: bill.id }, body });
    } else if (!navigator.onLine) {
      void save.queue(clientId, url, body);
    } else {
      create.mutate({
        path: { carId },
        body,
        headers: { 'Idempotency-Key': clientId },
      });
    }
  };

  const priceLabel = units.fuelPrice.inCurrency
    ? `${format.currencySymbol}/${units.volume.label}`
    : `ct/${units.volume.label}`;

  const computed = (
    value: number | undefined,
    unitLabel: string,
    digits = 2,
  ) =>
    value !== undefined
      ? t('bill.computed', { value: format.unit(value, unitLabel, digits) })
      : undefined;

  return (
    <Stack component="form" onSubmit={submit} noValidate spacing={2.5}>
      <Grid container spacing={2}>
        <DateVatFields
          date={form.date}
          onDateChange={(date) => set({ date })}
          vat={vat}
          error={touched ? errors.date : undefined}
        />
      </Grid>

      <Alert
        severity={touched && errors.amounts ? 'error' : 'info'}
        variant="outlined"
      >
        {t('bill.fuelHint', { context: units.volume.context })}
      </Alert>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.total')}
            value={form.total}
            onChange={(total) => set({ total })}
            suffix={` ${format.currencySymbol}`}
            max={MAX}
            helperText={
              derived.total !== undefined
                ? t('bill.computed', { value: format.money(derived.total) })
                : undefined
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.unit', { context: units.volume.context })}
            value={form.unit}
            onChange={(value) => set({ unit: value })}
            suffix={` ${units.volume.label}`}
            max={MAX}
            helperText={computed(derived.unit, units.volume.label)}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.pricePerUnit', {
              context: units.volume.context,
            })}
            value={form.pricePerUnit}
            onChange={(pricePerUnit) => set({ pricePerUnit })}
            suffix={` ${priceLabel}`}
            decimalScale={units.fuelPrice.digits}
            max={MAX}
            helperText={computed(
              derived.pricePerUnit,
              priceLabel,
              units.fuelPrice.digits,
            )}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.distance')}
            value={form.distance}
            onChange={(distance) => set({ distance })}
            suffix={` ${units.distance.label}`}
            decimalScale={1}
            max={MAX}
            helperText={
              consumption !== undefined
                ? t('bill.consumption', {
                    value: format.consumption(consumption),
                  })
                : t('bill.distanceHelp', { context: units.distance.context })
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.estimateConsumption')}
            value={form.estimateConsumption}
            onChange={(estimateConsumption) => set({ estimateConsumption })}
            suffix={` ${units.consumption.label}`}
            decimalScale={1}
            // l/100km or mpg
            max={1000}
            helperText={t('bill.estimateHelp')}
          />
        </Grid>
      </Grid>

      {overTank && (
        <Alert severity="warning" variant="outlined">
          {t('bill.overTank', {
            context: units.volume.context,
            unit: format.volume(liters),
            size: format.volume(car?.tankSize, 1),
          })}
        </Alert>
      )}

      <BillAttachments {...save.attachments} />

      <FormActions
        pending={create.isPending || update.isPending}
        onSaveAgain={save.markAgain}
        editing={!!bill}
      />
    </Stack>
  );
}
