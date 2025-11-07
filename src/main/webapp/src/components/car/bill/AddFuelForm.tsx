import { Button, Card, CardContent, Stack } from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';
import React, { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { addNewBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import BillNumericInput from './BillNumericInput';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers';

const MAX_NUMBER_INPUT = 10000;

export interface AddFuelFormProps {
  carId: number;
  navigate: (path: NavigateOptions) => void;
}

export default function AddFuelForm({ carId, navigate }: AddFuelFormProps) {
  const [form, setForm] = useState<{
    day: Dayjs;
    distance?: number;
    unit?: number;
    pricePerUnit?: number;
    estimate?: number;
  }>({
    day: dayjs(),
    distance: 0.0,
    unit: 0.0,
    pricePerUnit: 0.0,
    estimate: 0.0,
  });

  const { mutate, isPending } = useMutation({
    ...addNewBillMutation({
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
        day: dayjs(form.day, 'DD.MM.YYYY').format('YYYY-MM-DD'),
        distance: form.distance,
        unit: form.unit,
        pricePerUnit: form.pricePerUnit,
        estimate: form.estimate,
      },
    });
  };

  const formReset = () => {
    setForm({
      day: dayjs(),
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
          <DatePicker
            sx={{
              margin: 1,
            }}
            label="day"
            name="day"
            value={form.day}
            onChange={(e) => setForm({ ...form, day: e ?? dayjs() })}
            disableFuture
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label="Unit"
            name="unit"
            required
            suffix=" L"
            decimalScale={2}
            value={form.unit}
            onChange={(e) => setForm({ ...form, unit: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label="Price per unit"
            name="pricePerUnit"
            required
            suffix=" ct"
            value={form.pricePerUnit}
            onChange={(e) => setForm({ ...form, pricePerUnit: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label="Distance driven"
            name="distance"
            suffix=" km"
            value={form.distance}
            onChange={(e) => setForm({ ...form, distance: e.floatValue })}
          />
          <BillNumericInput
            maxInput={MAX_NUMBER_INPUT}
            label="Estimated consumption"
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
              Save
            </Button>
            <Button
              variant="outlined"
              onClick={() =>
                navigate({ to: `/car/$id`, params: { id: `${carId}` } })
              }
            >
              Cancel
            </Button>
          </Stack>
        </Stack>
      </CardContent>
    </Card>
  );
}
