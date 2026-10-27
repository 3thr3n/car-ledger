import { Grid, Stack, TextField } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, MaintenanceBill } from '@/generated';
import {
  getMyCarOptions,
  addNewMaintenanceBillMutation,
  updateMaintenanceBillMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { hasValue, toApiDate, useFormat } from '@/utils/format';
import { odometerToApi, odometerToField, Units } from '@/utils/units';
import useVatCountry from '@/hooks/useVatCountry';
import NumberField from './NumberField';
import DateVatFields from './DateVatFields';
import FormActions from './FormActions';
import BillAttachments from './BillAttachments';
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

const fromBill = (bill: MaintenanceBill, units: Units): MaintenanceState => ({
  date: dayjs(bill.date),
  workshop: bill.workshop ?? '',
  description: bill.description ?? '',
  odometer: hasValue(bill.odometer)
    ? odometerToField(units, bill.odometer)
    : undefined,
  laborCost: hasValue(bill.laborCost) ? bill.laborCost : undefined,
  partsCost: hasValue(bill.partsCost) ? bill.partsCost : undefined,
  total: bill.total,
});

export default function MaintenanceForm({
  carId,
  bill,
}: {
  carId: number;
  /** bill to edit, a new one is created without */
  bill?: MaintenanceBill;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  const vat = useVatCountry(bill?.vatRate);
  const [form, setForm] = useState<MaintenanceState>(() =>
    bill ? fromBill(bill, format.units) : empty(),
  );
  const [touched, setTouched] = useState(false);

  const save = useSaveBill(
    carId,
    BillType.MAINTENANCE,
    () => {
      setForm(empty());
      setTouched(false);
    },
    bill?.id,
  );
  const create = useMutation({
    ...addNewMaintenanceBillMutation({ client: localClient }),
    onSuccess: save.onSuccess,
    onError: save.onError,
  });
  const update = useMutation({
    ...updateMaintenanceBillMutation({ client: localClient }),
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

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (errors.date || errors.total) return;

    const body = {
      date: toApiDate(form.date),
      vatRate: vat.vatRate,
      total,
      workshop: form.workshop.trim() || undefined,
      description: form.description.trim() || undefined,
      odometer: odometerToApi(format.units, form.odometer),
      laborCost: form.laborCost,
      partsCost: form.partsCost,
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
        <Grid size={{ xs: 12, sm: 6 }}>
          <TextField
            label={t('bill.fields.workshop')}
            value={form.workshop}
            onChange={(e) => set({ workshop: e.target.value })}
            slotProps={{ htmlInput: { maxLength: 100 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6 }}>
          <NumberField
            label={t('bill.fields.odometer')}
            value={form.odometer}
            onChange={(odometer) => set({ odometer })}
            decimalScale={0}
            suffix={` ${format.units.distance.label}`}
            max={10_000_000}
            helperText={
              car &&
              t('bill.currentOdometer', {
                km: format.distance(car.currentOdometer.value),
              })
            }
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
            slotProps={{ htmlInput: { maxLength: 1000 } }}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.laborCost')}
            value={form.laborCost}
            onChange={(laborCost) => set({ laborCost })}
            suffix={` ${format.currencySymbol}`}
            max={MAX}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.partsCost')}
            value={form.partsCost}
            onChange={(partsCost) => set({ partsCost })}
            suffix={` ${format.currencySymbol}`}
            max={MAX}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 4 }}>
          <NumberField
            label={t('bill.fields.total')}
            value={form.total}
            onChange={(value) => set({ total: value })}
            suffix={` ${format.currencySymbol}`}
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

      <BillAttachments {...save.attachments} />

      <FormActions
        pending={create.isPending || update.isPending}
        onSaveAgain={save.markAgain}
        editing={!!bill}
      />
    </Stack>
  );
}
