import { Button, Card, CardContent, Stack } from '@mui/material';
import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { addNewFuelBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import BillNumericInput from '../BillNumericInput';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers';
import CountrySelection from '@/components/car/bill/CountrySelection';
import countryVat from 'country-vat';
import { useTranslation } from 'react-i18next';
import { useRouter } from '@tanstack/react-router';

const MAX_NUMBER_INPUT = 10000;

export interface AddFuelFormProps {
  carId: number;
}

export default function AddFuelForm({ carId }: AddFuelFormProps) {
  const { t } = useTranslation();
  const router = useRouter();
  // TODO: Get the initial state from the backend (User preference!)
  const [countryCode, setCountryCode] = useState('DE');

  const [form, setForm] = useState<{
    date: Dayjs;
    distance?: number;
    unit?: number;
    pricePerUnit?: number;
    estimate?: number;
  }>({
    date: dayjs(),
    distance: 0.0,
    unit: 0.0,
    pricePerUnit: 0.0,
    estimate: 0.0,
  });

  const { mutate, isPending } = useMutation({
    ...addNewFuelBillMutation({
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

  const handleSave = () => {
    mutate({
      path: {
        carId,
      },
      body: {
        date: dayjs(form.date, 'DD.MM.YYYY').format('YYYY-MM-DD'),
        distance: form.distance,
        unit: form.unit,
        pricePerUnit: form.pricePerUnit,
        estimateConsumption: form.estimate,
        vatRate: countryVat(countryCode)! * 100,
      },
    });
  };

  const formReset = () => {
    setForm({
      date: dayjs(),
      distance: 0.0,
      estimate: 0.0,
      unit: 0.0,
      pricePerUnit: 0.0,
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
            maxInput={MAX_NUMBER_INPUT}
            label={t('app.car.common.unit')}
            name="unit"
            required
            suffix=" L"
            decimalScale={2}
            value={form.unit}
            onChange={(e) => setForm({ ...form, unit: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label={t('app.car.common.pricePerUnit')}
            name="pricePerUnit"
            required
            suffix=" ct"
            value={form.pricePerUnit}
            onChange={(e) => setForm({ ...form, pricePerUnit: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label={t('app.car.common.distance')}
            name="distance"
            suffix=" km"
            value={form.distance}
            onChange={(e) => setForm({ ...form, distance: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label={t('app.car.common.estimateConsumption')}
            name="estimate"
            suffix=" L"
            value={form.estimate}
            onChange={(e) => setForm({ ...form, estimate: e.floatValue })}
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
