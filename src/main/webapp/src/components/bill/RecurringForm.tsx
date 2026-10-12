import { Alert, Grid, MenuItem, Stack, TextField } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillCategory, BillInterval, BillType } from '@/generated';
import { addNewRecurringBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate, useFormat } from '@/utils/format';
import NumberField from './NumberField';
import FormActions from './FormActions';
import useSaveBill from './useSaveBill';

interface RecurringState {
  name: string;
  description: string;
  category: BillCategory;
  interval: BillInterval;
  startDate: Dayjs | null;
  endDate: Dayjs | null;
  amount?: number;
}

const empty = (): RecurringState => ({
  name: '',
  description: '',
  category: BillCategory.INSURANCE,
  interval: BillInterval.MONTHLY,
  startDate: dayjs(),
  endDate: null,
});

export default function RecurringForm({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const [form, setForm] = useState<RecurringState>(empty);
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(carId, BillType.RECURRING, () => {
    setForm(empty());
    setTouched(false);
  });
  const mutation = useMutation({
    ...addNewRecurringBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  const set = (patch: Partial<RecurringState>) =>
    setForm((f) => ({ ...f, ...patch }));

  // same rules as the backend validation
  const errors = {
    name: !form.name.trim() ? t('bill.errors.name') : undefined,
    startDate: !form.startDate?.isValid()
      ? t('bill.errors.startDate')
      : undefined,
    endDate:
      form.endDate && form.startDate && form.startDate.isAfter(form.endDate)
        ? t('bill.errors.dateOrder')
        : undefined,
    amount: !form.amount ? t('bill.errors.amount') : undefined,
  };
  const valid = Object.values(errors).every((e) => !e);

  const startsInPast = form.startDate?.isBefore(dayjs(), 'day');

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!valid) return;

    mutation.mutate({
      path: { carId },
      body: {
        name: form.name.trim(),
        description: form.description.trim() || undefined,
        category: form.category,
        billInterval: form.interval,
        startDate: toApiDate(form.startDate),
        endDate: form.endDate?.isValid() ? toApiDate(form.endDate) : undefined,
        amount: form.amount,
      },
    });
  };

  return (
    <Stack component="form" onSubmit={submit} noValidate spacing={2.5}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label={t('bill.fields.name')}
            placeholder={t('bill.recurringPlaceholder')}
            value={form.name}
            onChange={(e) => set({ name: e.target.value })}
            required
            error={touched && !!errors.name}
            helperText={touched && errors.name}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.amount')}
            value={form.amount}
            onChange={(amount) => set({ amount })}
            suffix={` ${format.currencySymbol}`}
            max={1_000_000}
            required
            error={touched ? errors.amount : undefined}
            helperText={t('bill.amountHelp')}
          />
        </Grid>
        <Grid size={12}>
          <TextField
            label={t('bill.fields.description')}
            value={form.description}
            onChange={(e) => set({ description: e.target.value })}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            select
            label={t('bill.fields.category')}
            value={form.category}
            onChange={(e) => set({ category: e.target.value as BillCategory })}
            required
          >
            {Object.values(BillCategory).map((category) => (
              <MenuItem key={category} value={category}>
                {t(`bill.categories.${category}`)}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            select
            label={t('bill.fields.interval')}
            value={form.interval}
            onChange={(e) => set({ interval: e.target.value as BillInterval })}
            required
          >
            {Object.values(BillInterval).map((interval) => (
              <MenuItem key={interval} value={interval}>
                {t(`bill.intervals.${interval}`)}
              </MenuItem>
            ))}
          </TextField>
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <DatePicker
            label={t('bill.fields.startDate')}
            value={form.startDate}
            onChange={(startDate) => set({ startDate })}
            slotProps={{
              textField: {
                required: true,
                error: touched && !!errors.startDate,
                helperText: touched && errors.startDate,
              },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <DatePicker
            label={t('bill.fields.endDate')}
            value={form.endDate}
            onChange={(endDate) => set({ endDate })}
            minDate={form.startDate ?? undefined}
            slotProps={{
              field: { clearable: true },
              textField: {
                error: touched && !!errors.endDate,
                helperText:
                  (touched && errors.endDate) || t('bill.endDateHelp'),
              },
            }}
          />
        </Grid>
      </Grid>

      {startsInPast && (
        <Alert severity="info" variant="outlined">
          {t('bill.recurringPastHint')}
        </Alert>
      )}

      <FormActions pending={mutation.isPending} onSaveAgain={save.markAgain} />
    </Stack>
  );
}
