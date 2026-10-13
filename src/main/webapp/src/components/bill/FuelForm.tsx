import { Alert, Grid, Stack } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, FuelBill } from '@/generated';
import {
  addNewFuelBillMutation,
  updateFuelBillMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { hasValue, toApiDate, useFormat } from '@/utils/format';
import { derive, isSet } from '@/utils/fuel';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import DateVatFields from './DateVatFields';
import FormActions from './FormActions';
import BillAttachments from './BillAttachments';
import useSaveBill from './useSaveBill';

const MAX = 100_000;

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
 * Starts with total and liters only, so changing one of them calculates the price again
 */
const fromBill = (bill: FuelBill): FuelState => ({
  date: dayjs(bill.date),
  total: bill.total,
  unit: bill.unit,
  distance: hasValue(bill.distance) ? bill.distance : undefined,
  estimateConsumption: hasValue(bill.estimateConsumption)
    ? bill.estimateConsumption
    : undefined,
});

export default function FuelForm({
  carId,
  bill,
}: {
  carId: number;
  /** bill to edit, a new one is created without */
  bill?: FuelBill;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const vat = useVatCountry(bill?.vatRate);
  const [form, setForm] = useState<FuelState>(() =>
    bill ? fromBill(bill) : empty(),
  );
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(
    carId,
    BillType.FUEL,
    () => {
      setForm(empty());
      setTouched(false);
    },
    bill?.id,
  );
  const create = useMutation({
    ...addNewFuelBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });
  const update = useMutation({
    ...updateFuelBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  const set = (patch: Partial<FuelState>) =>
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
  const consumption =
    isSet(unit) && isSet(form.distance)
      ? (unit! / form.distance!) * 100
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
      distance: form.distance,
      estimateConsumption: form.estimateConsumption,
    };
    if (bill?.id !== undefined) {
      update.mutate({ path: { carId, billId: bill.id }, body });
    } else {
      create.mutate({ path: { carId }, body });
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
        {t('bill.fuelHint')}
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
            label={t('bill.fields.unit')}
            value={form.unit}
            onChange={(value) => set({ unit: value })}
            suffix=" l"
            max={MAX}
            helperText={computed(derived.unit, 'l')}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.pricePerUnit')}
            value={form.pricePerUnit}
            onChange={(pricePerUnit) => set({ pricePerUnit })}
            suffix=" ct/l"
            decimalScale={1}
            max={MAX}
            helperText={computed(derived.pricePerUnit, 'ct/l', 1)}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.distance')}
            value={form.distance}
            onChange={(distance) => set({ distance })}
            suffix=" km"
            decimalScale={1}
            max={MAX}
            helperText={
              consumption !== undefined
                ? t('bill.consumption', {
                    value: format.unit(consumption, 'l/100km'),
                  })
                : t('bill.distanceHelp')
            }
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.estimateConsumption')}
            value={form.estimateConsumption}
            onChange={(estimateConsumption) => set({ estimateConsumption })}
            suffix=" l/100km"
            decimalScale={1}
            max={100}
            helperText={t('bill.estimateHelp')}
          />
        </Grid>
      </Grid>

      <BillAttachments {...save.attachments} />

      <FormActions
        pending={create.isPending || update.isPending}
        onSaveAgain={save.markAgain}
        editing={!!bill}
      />
    </Stack>
  );
}
