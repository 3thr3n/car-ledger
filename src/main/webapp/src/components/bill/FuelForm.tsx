import { Alert, Grid, Stack } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { addNewFuelBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate, useFormat } from '@/utils/format';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import VatCountrySelect from './VatCountrySelect';
import FormActions from './FormActions';
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

const isSet = (value?: number) => value !== undefined && value > 0;

/**
 * Same calculation as the backend: two of total, unit and price per unit define the third
 */
function derive({ total, unit, pricePerUnit }: FuelState) {
  if (isSet(unit) && isSet(pricePerUnit) && !isSet(total)) {
    return { total: (unit! * pricePerUnit!) / 100 };
  }
  if (isSet(total) && isSet(pricePerUnit) && !isSet(unit)) {
    return { unit: total! / (pricePerUnit! / 100) };
  }
  if (isSet(total) && isSet(unit) && !isSet(pricePerUnit)) {
    return { pricePerUnit: (total! * 100) / unit! };
  }
  return {};
}

export default function FuelForm({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const vat = useVatCountry();
  const [form, setForm] = useState<FuelState>(empty);
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(carId, BillType.FUEL, () => {
    setForm(empty());
    setTouched(false);
  });
  const mutation = useMutation({
    ...addNewFuelBillMutation({ client: localClient }),
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

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.amounts) return;

    mutation.mutate({
      path: { carId },
      body: {
        date: toApiDate(form.date),
        vatRate: vat.vatRate,
        total: form.total,
        unit: form.unit,
        pricePerUnit: form.pricePerUnit,
        distance: form.distance,
        estimateConsumption: form.estimateConsumption,
      },
    });
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
        <Grid size={{ xs: 12, sm: 6 }}>
          <DatePicker
            label={t('bill.fields.date')}
            value={form.date}
            onChange={(date) => set({ date })}
            disableFuture
            slotProps={{
              textField: {
                required: true,
                error: touched && !!errors.date,
                helperText: touched && errors.date,
              },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <VatCountrySelect value={vat.country} onChange={vat.setCountry} />
        </Grid>
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
            suffix=" €"
            max={MAX}
            helperText={computed(derived.total, '€')}
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

      <FormActions pending={mutation.isPending} onSaveAgain={save.markAgain} />
    </Stack>
  );
}
