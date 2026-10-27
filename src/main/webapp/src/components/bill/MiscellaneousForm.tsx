import { Grid, Stack, TextField } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, MiscellaneousBill } from '@/generated';
import {
  addNewMiscellaneousBillMutation,
  updateMiscellaneousBillMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate, useFormat } from '@/utils/format';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import DateVatFields from './DateVatFields';
import FormActions from './FormActions';
import BillAttachments from './BillAttachments';
import useSaveBill from './useSaveBill';

interface MiscellaneousState {
  date: Dayjs | null;
  description: string;
  total?: number;
}

const empty = (): MiscellaneousState => ({ date: dayjs(), description: '' });

const fromBill = (bill: MiscellaneousBill): MiscellaneousState => ({
  date: dayjs(bill.date),
  description: bill.description ?? '',
  total: bill.total,
});

export default function MiscellaneousForm({
  carId,
  bill,
}: {
  carId: number;
  /** bill to edit, a new one is created without */
  bill?: MiscellaneousBill;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const vat = useVatCountry(bill?.vatRate);
  const [form, setForm] = useState<MiscellaneousState>(() =>
    bill ? fromBill(bill) : empty(),
  );
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(
    carId,
    BillType.MISCELLANEOUS,
    () => {
      setForm(empty());
      setTouched(false);
    },
    bill?.id,
  );
  const create = useMutation({
    ...addNewMiscellaneousBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });
  const update = useMutation({
    ...updateMiscellaneousBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });

  const set = (patch: Partial<MiscellaneousState>) =>
    setForm((f) => ({ ...f, ...patch }));

  const errors = {
    date: !form.date?.isValid() ? t('bill.errors.date') : undefined,
    total: !form.total ? t('bill.errors.total') : undefined,
  };

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.total) return;

    const body = {
      date: toApiDate(form.date),
      vatRate: vat.vatRate,
      total: form.total,
      description: form.description.trim() || undefined,
    };
    if (bill?.id !== undefined) {
      update.mutate({ path: { carId, billId: bill.id }, body });
    } else {
      create.mutate({ path: { carId }, body });
    }
  };

  return (
    <Stack component="form" onSubmit={submit} noValidate spacing={2.5}>
      <Grid container spacing={2}>
        <DateVatFields
          date={form.date}
          onDateChange={(date) => set({ date })}
          vat={vat}
          error={touched ? errors.date : undefined}
        />
        <Grid size={{ xs: 12, sm: 8 }}>
          <TextField
            label={t('bill.fields.description')}
            placeholder={t('bill.miscellaneousPlaceholder')}
            value={form.description}
            onChange={(e) => set({ description: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 1000 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.total')}
            value={form.total}
            onChange={(total) => set({ total })}
            suffix={` ${format.currencySymbol}`}
            max={1_000_000}
            required
            error={touched ? errors.total : undefined}
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
