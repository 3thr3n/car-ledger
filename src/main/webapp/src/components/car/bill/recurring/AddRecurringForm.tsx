import { useTranslation } from 'react-i18next';
import { useRouter } from '@tanstack/react-router';
import { useState } from 'react';
import dayjs, { Dayjs } from 'dayjs';
import { useMutation } from '@tanstack/react-query';
import { addNewRecurringBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import {
  Button,
  Card,
  CardContent,
  MenuItem,
  Select,
  Stack,
  TextField,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import BillNumericInput from '../BillNumericInput';
import { BillCategory, BillInterval } from '@/generated';

export interface AddRecurringFormProps {
  carId: number;
}

export default function AddRecurringForm({ carId }: AddRecurringFormProps) {
  const { t } = useTranslation();
  const router = useRouter();

  const [form, setForm] = useState<{
    name: string;
    description?: string;
    interval?: BillInterval;
    category?: BillCategory;
    startDate: Dayjs;
    endDate?: Dayjs;
    amount?: number;
  }>({
    name: '',
    startDate: dayjs(),
  });

  const { mutate, isPending } = useMutation({
    ...addNewRecurringBillMutation({
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
      name: '',
      startDate: dayjs(),
      endDate: undefined,
      amount: undefined,
      interval: undefined,
      category: undefined,
      description: '',
    });
  };

  const handleSave = () => {
    mutate({
      path: {
        carId,
      },
      body: {
        name: form.name,
        description: form.description,
        billInterval: form.interval,
        category: form.category,
        startDate: dayjs(form.startDate, 'DD.MM.YYYY').format('YYYY-MM-DD'),
        endDate:
          form.endDate &&
          dayjs(form.endDate, 'DD.MM.YYYY').format('YYYY-MM-DD'),
        amount: form.amount,
      },
    });
  };

  return (
    <Card>
      <CardContent>
        <Stack spacing={2}>
          <TextField
            placeholder={t('app.car.common.name')}
            label={t('app.car.common.name')}
            value={form.name}
            // onBlur={handleValidate}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            fullWidth
            // error={!!errors.name}
            // helperText={errors.name}
          />
          <TextField
            placeholder={t('app.car.common.description')}
            label={t('app.car.common.description')}
            value={form.description}
            // onBlur={handleValidate}
            onChange={(e) => setForm({ ...form, description: e.target.value })}
            fullWidth
            // error={!!errors.name}
            // helperText={errors.name}
          />
          <Select
            variant="outlined"
            value={form.category}
            onChange={(e) => setForm({ ...form, category: e.target.value })}
            sx={{ margin: 1 }}
          >
            {Object.values(BillCategory).map((x) => (
              <MenuItem key={x} value={x}>
                {x}
              </MenuItem>
            ))}
          </Select>
          <Select
            variant="outlined"
            value={form.interval}
            onChange={(e) => setForm({ ...form, interval: e.target.value })}
            sx={{ margin: 1 }}
          >
            {Object.values(BillInterval).map((x) => (
              <MenuItem key={x} value={x}>
                {x}
              </MenuItem>
            ))}
          </Select>
          <DatePicker
            sx={{
              margin: 1,
            }}
            label={t('app.car.common.startDate')}
            name="date"
            value={form.startDate}
            onChange={(e) => setForm({ ...form, startDate: e ?? dayjs() })}
            disableFuture
          />
          <DatePicker
            sx={{
              margin: 1,
            }}
            label={t('app.car.common.endDate')}
            name="date"
            value={form.endDate}
            onChange={(e) => setForm({ ...form, endDate: e ?? dayjs() })}
          />
          <BillNumericInput
            label={t('app.car.common.amount')}
            name="total"
            required
            suffix=" €"
            decimalScale={2}
            value={form.amount}
            onChange={(e) => setForm({ ...form, amount: e.floatValue })}
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
