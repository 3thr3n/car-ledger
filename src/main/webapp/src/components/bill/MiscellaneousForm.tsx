import { Grid, Stack, TextField } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { addNewMiscellaneousBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate } from '@/utils/format';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import VatCountrySelect from './VatCountrySelect';
import FormActions from './FormActions';
import useSaveBill from './useSaveBill';

interface MiscellaneousState {
  date: Dayjs | null;
  description: string;
  total?: number;
}

const empty = (): MiscellaneousState => ({ date: dayjs(), description: '' });

export default function MiscellaneousForm({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const vat = useVatCountry();
  const [form, setForm] = useState<MiscellaneousState>(empty);
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(carId, BillType.MISCELLANEOUS, () => {
    setForm(empty());
    setTouched(false);
  });
  const mutation = useMutation({
    ...addNewMiscellaneousBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  const set = (patch: Partial<MiscellaneousState>) =>
    setForm((f) => ({ ...f, ...patch }));

  const errors = {
    date: !form.date?.isValid() ? t('bill.errors.date') : undefined,
    total: !form.total ? t('bill.errors.total') : undefined,
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
        total: form.total,
        description: form.description.trim() || undefined,
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
        <Grid size={{ xs: 12, sm: 8 }}>
          <TextField
            label={t('bill.fields.description')}
            placeholder={t('bill.miscellaneousPlaceholder')}
            value={form.description}
            onChange={(e) => set({ description: e.target.value })}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.total')}
            value={form.total}
            onChange={(total) => set({ total })}
            suffix=" €"
            max={1_000_000}
            required
            error={touched ? errors.total : undefined}
          />
        </Grid>
      </Grid>

      <FormActions pending={mutation.isPending} onSaveAgain={save.markAgain} />
    </Stack>
  );
}
