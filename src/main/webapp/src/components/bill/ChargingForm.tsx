import { Alert, Grid, MenuItem, Stack, TextField } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, ChargingBill, ChargingLocation } from '@/generated';
import {
  addNewChargingBillMutation,
  getMyCarOptions,
  updateChargingBillMutation,
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
/**
 * charges up to 20 % above the battery capacity are no warning: a wallbox or
 * station counts the charging losses too
 */
const BATTERY_TOLERANCE = 1.2;

interface ChargingState {
  date: Dayjs | null;
  total?: number;
  unit?: number;
  pricePerUnit?: number;
  distance?: number;
  /** empty string for none, the select needs a value */
  location: ChargingLocation | '';
  stateOfCharge?: number;
}

const empty = (): ChargingState => ({ date: dayjs(), location: '' });

/**
 * Starts with total and kWh only, so changing one of them calculates the price again
 */
const fromBill = (bill: ChargingBill, units: Units): ChargingState => ({
  date: dayjs(bill.date),
  total: bill.total,
  unit: bill.unit,
  distance: hasValue(bill.distance)
    ? toField(bill.distance, units.distance.fromKm, 1)
    : undefined,
  location: bill.location ?? '',
  stateOfCharge: bill.stateOfCharge,
});

export default function ChargingForm({
  carId,
  bill,
  queued,
}: {
  carId: number;
  /** bill to edit, a new one is created without */
  bill?: ChargingBill;
  /** a new bill of the offline queue the server rejected, to correct */
  queued?: QueuedRequest;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { units } = format;
  const vat = useVatCountry(bill?.vatRate);
  const [form, setForm] = useState<ChargingState>(() => {
    if (bill) return fromBill(bill, units);
    // the body is in the units of the API, like a saved bill
    if (queued) return fromBill(queued.body as unknown as ChargingBill, units);
    return empty();
  });
  const [touched, setTouched] = useState(false);
  // id of the new bill: sent again (e.g. from the offline queue), it is
  // created once
  const [clientId, setClientId] = useState(
    () => queued?.id ?? crypto.randomUUID(),
  );
  const url = `/api/bill/${carId}/charging`;

  const save = useSaveBill(
    carId,
    BillType.CHARGING,
    () => {
      setForm(empty());
      setTouched(false);
      setClientId(crypto.randomUUID());
    },
    bill?.id,
  );
  const create = useMutation({
    ...addNewChargingBillMutation({ client: localClient }),
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
    ...updateChargingBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );

  const set = (patch: Partial<ChargingState>) =>
    setForm((f) => ({ ...f, ...patch }));
  const derived = derive(form);
  const amountCount = [form.total, form.unit, form.pricePerUnit].filter(
    isSet,
  ).length;

  const errors = {
    date: !form.date?.isValid() ? t('bill.errors.date') : undefined,
    amounts: amountCount < 2 ? t('bill.errors.atLeastTwo') : undefined,
  };

  const unit = form.unit ?? derived.unit;
  const overBattery =
    isSet(unit) && car?.batteryCapacity
      ? unit! > car.batteryCapacity * BATTERY_TOLERANCE
      : false;
  // kWh/100km like the API, shown in the units of the account
  const consumption =
    isSet(unit) && isSet(form.distance)
      ? (unit! / units.distance.toKm(form.distance!)) * 100
      : undefined;

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.amounts) return;

    const body = {
      date: toApiDate(form.date),
      vatRate: vat.vatRate,
      total: form.total,
      unit: form.unit,
      pricePerUnit: form.pricePerUnit,
      // the API takes km
      distance: convert(form.distance, units.distance.toKm),
      location: form.location || undefined,
      stateOfCharge: form.stateOfCharge,
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
        {t('bill.chargingHint')}
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
            label={t('bill.fields.chargedUnit')}
            value={form.unit}
            onChange={(value) => set({ unit: value })}
            suffix=" kWh"
            max={MAX}
            helperText={computed(derived.unit, 'kWh')}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.chargingPricePerUnit')}
            value={form.pricePerUnit}
            onChange={(pricePerUnit) => set({ pricePerUnit })}
            suffix=" ct/kWh"
            decimalScale={1}
            max={MAX}
            helperText={computed(derived.pricePerUnit, 'ct/kWh', 1)}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
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
                    value: format.chargingConsumption(consumption),
                  })
                : t('bill.chargingDistanceHelp', {
                    context: units.distance.context,
                  })
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <TextField
            select
            label={t('bill.fields.location')}
            value={form.location}
            onChange={(e) =>
              set({ location: e.target.value as ChargingLocation | '' })
            }
          >
            <MenuItem value="">{t('bill.locations.none')}</MenuItem>
            {Object.values(ChargingLocation).map((location) => (
              <MenuItem key={location} value={location}>
                {t(`bill.locations.${location}`)}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.stateOfCharge')}
            value={form.stateOfCharge}
            onChange={(stateOfCharge) => set({ stateOfCharge })}
            suffix=" %"
            decimalScale={0}
            max={100}
            helperText={t('bill.stateOfChargeHelp')}
          />
        </Grid>
      </Grid>

      {overBattery && (
        <Alert severity="warning" variant="outlined">
          {t('bill.overBattery', {
            unit: format.unit(unit, 'kWh'),
            size: format.unit(car?.batteryCapacity, 'kWh', 1),
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
