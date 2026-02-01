import { useTranslation } from 'react-i18next';
import { useRouter } from '@tanstack/react-router';
import { useState } from 'react';
import dayjs, { Dayjs } from 'dayjs';
import { useMutation } from '@tanstack/react-query';
import { addNewMaintenanceBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import countryVat from 'country-vat';
import { Button, Card, CardContent, Stack, TextField } from '@mui/material';
import CountrySelection from './CountrySelection';
import { DatePicker } from '@mui/x-date-pickers';
import BillNumericInput from './BillNumericInput';

const MAX_NUMBER_INPUT = 10000;

export interface AddMaintenanceFormProps {
  carId: number;
}

export default function AddMaintenanceForm({ carId }: AddMaintenanceFormProps) {
  const { t } = useTranslation();
  const router = useRouter();
  // TODO: Get the initial state from the backend (User preference!)
  const [countryCode, setCountryCode] = useState('DE');

  const [form, setForm] = useState<{
    date: Dayjs;
    total?: number;
    vatRate?: number;
    odometer?: number;
    laborCost?: number;
    partsCost?: number;
    description?: string;
    workshop?: string;
  }>({
    date: dayjs(),
    total: 0.0,
    vatRate: 0.0,
    odometer: 0.0,
    description: '',
    workshop: '',
  });

  const { mutate, isPending } = useMutation({
    ...addNewMaintenanceBillMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Bill saved!');
      formReset();
    },
    onError: (error) => {
      if (error instanceof BackendError) {
        console.warn('Non successful response', error.status);
      }
    },
  });

  const formReset = () => {
    setForm({
      date: dayjs(),
      total: 0.0,
      vatRate: 0.0,
      odometer: 0.0,
      description: '',
      workshop: '',
    });
  };

  const handleSave = () => {
    mutate({
      path: {
        carId,
      },
      body: {
        date: dayjs(form.date, 'DD.MM.YYYY').format('YYYY-MM-DD'),
        total: form.total,
        vatRate: countryVat(countryCode)! * 100,
        odometer: form.odometer,
        description: form.description,
        workshop: form.workshop,
      },
    });
  };

  return (
    <Card>
      <CardContent>
        <Stack spacing={2}>
          <CountrySelection
            value={countryCode}
            onChange={(countryCode) => setCountryCode(countryCode)}
          />
          <DatePicker
            sx={{
              margin: 1,
            }}
            label={t('app.car.common.date')}
            name="date"
            value={form.date}
            onChange={(e) => setForm({ ...form, date: e ?? dayjs() })}
            disableFuture
          />
          <BillNumericInput
            label={t('app.car.common.total')}
            name="total"
            required
            suffix=" €"
            decimalScale={2}
            value={form.total}
            onChange={(e) => setForm({ ...form, total: e.floatValue })}
          />
          <TextField
            placeholder={t('app.car.maintenance.table.workshop')}
            label={t('app.car.maintenance.table.workshop')}
            value={form.workshop}
            // onBlur={handleValidate}
            onChange={(e) => setForm({ ...form, workshop: e.target.value })}
            fullWidth
            // error={!!errors.name}
            // helperText={errors.name}
          />
          <TextField
            placeholder={t('app.car.maintenance.table.description')}
            label={t('app.car.maintenance.table.description')}
            value={form.description}
            // onBlur={handleValidate}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            fullWidth
            // error={!!errors.name}
            // helperText={errors.name}
          />
          <BillNumericInput
            label={t('app.car.common.odometer')}
            name="odometer"
            required
            suffix=" km"
            value={form.odometer}
            onChange={(e) => setForm({ ...form, odometer: e.floatValue })}
          />
          <Stack direction="row" spacing={2} mt={2}>
            <Button
              variant="contained"
              onClick={() => handleSave()}
              disabled={isPending}
            >
              {t('app.button.save')}
            </Button>
            <Button variant="outlined" onClick={() => router.history.back()}>
              {t('app.button.cancel')}
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}
