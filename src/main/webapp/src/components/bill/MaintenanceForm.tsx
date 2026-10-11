import { Grid, Stack, TextField } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { addNewMaintenanceBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate, useFormat } from '@/utils/format';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import VatCountrySelect from './VatCountrySelect';
import FormActions from './FormActions';
import useSaveBill from './useSaveBill';

const MAX = 1_000_000;

interface MaintenanceState {
  date: Dayjs | null;
  workshop: string;
  description: string;
  odometer?: number;
  laborCost?: number;
  partsCost?: number;
  total?: number;
}

const empty = (): MaintenanceState => ({
  date: dayjs(),
  workshop: '',
  description: '',
});

export default function MaintenanceForm({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const vat = useVatCountry();
  const [form, setForm] = useState<MaintenanceState>(empty);
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(carId, BillType.MAINTENANCE, () => {
    setForm(empty());
    setTouched(false);
  });
  const mutation = useMutation({
    ...addNewMaintenanceBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  const set = (patch: Partial<MaintenanceState>) =>
    setForm((f) => ({ ...f, ...patch }));

  // without an explicit total, labor and parts add up to it
  const partSum =
    form.laborCost !== undefined || form.partsCost !== undefined
      ? (form.laborCost ?? 0) + (form.partsCost ?? 0)
      : undefined;
  const total = form.total ?? partSum;

  const errors = {
    date: !form.date?.isValid() ? t('bill.errors.date') : undefined,
    total: !total ? t('bill.errors.total') : undefined,
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.total) return;

    mutation.mutate({
      path: { carId },
      body: {
        date: toApiDate(form.date),
        vatRate: vat.vatRate,
        total,
        workshop: form.workshop.trim() || undefined,
        description: form.description.trim() || undefined,
        odometer: form.odometer,
        laborCost: form.laborCost,
        partsCost: form.partsCost,
      },
    });
  };

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
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label={t('bill.fields.workshop')}
            value={form.workshop}
            onChange={(e) => set({ workshop: e.target.value })}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.odometer')}
            value={form.odometer}
            onChange={(odometer) => set({ odometer })}
            decimalScale={0}
            suffix=" km"
            max={10_000_000}
          />
        </Grid>
        <Grid size={12}>
          <TextField
            label={t('bill.fields.description')}
            placeholder={t('bill.maintenancePlaceholder')}
            value={form.description}
            onChange={(e) => set({ description: e.target.value })}
            multiline
            minRows={2}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.laborCost')}
            value={form.laborCost}
            onChange={(laborCost) => set({ laborCost })}
            suffix=" €"
            max={MAX}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.partsCost')}
            value={form.partsCost}
            onChange={(partsCost) => set({ partsCost })}
            suffix=" €"
            max={MAX}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.total')}
            value={form.total}
            onChange={(value) => set({ total: value })}
            suffix=" €"
            max={MAX}
            required
            error={touched ? errors.total : undefined}
            helperText={
              form.total === undefined && partSum !== undefined
                ? t('bill.computed', { value: format.money(partSum) })
                : t('bill.totalHelp')
            }
          />
        </Grid>
      </Grid>

      <FormActions pending={mutation.isPending} onSaveAgain={save.markAgain} />
    </Stack>
  );
}
