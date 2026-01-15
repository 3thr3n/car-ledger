import { NavigateOptions } from '@tanstack/router-core';
import React, { useState } from 'react';
import {
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { NumericFormat } from 'react-number-format';
import { useMutation } from '@tanstack/react-query';
import { createCarMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import { useTranslation } from 'react-i18next';

export interface CarNewPageProperties {
  navigate: (path: NavigateOptions) => void;
}

export default function CarNewPage({ navigate }: CarNewPageProperties) {
  const { t } = useTranslation();

  const [name, setName] = useState('');
  const [year, setYear] = useState<number>(2025);
  const [km, setKm] = useState<number>(0);
  const [errors, setErrors] = useState<{
    name?: string;
    year?: string;
    km?: string;
  }>({});

  const currentYear = new Date().getFullYear();

  const { mutate } = useMutation({
    ...createCarMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Car saved!');
    },
    onSettled: () => {
      navigate({ to: '..' });
    },
    onError: (error: BackendError) => {
      if (error.status === 400) {
        toast.error(error.message);
      } else {
        console.warn('Non successful response', error.status);
        toast.error('Backend failed!');
      }
    },
  });

  const handleValidate = () => {
    const newErrors: typeof errors = {};

    if (!name.trim()) {
      newErrors.name = 'Car name is required';
    }
    if (!year || year < 1900 || year > currentYear) {
      newErrors.year = 'Please enter a valid year';
    }
    if (km < 0) {
      newErrors.km = 'Odometer must be a positive number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (!handleValidate()) return;
    mutate({ body: { name, year, odometer: km } });
  };

  const handleCancel = () => {
    navigate({ to: '/car' });
  };

  return (
    <Container maxWidth="sm" sx={{ py: 5 }}>
      <Typography variant="h4" gutterBottom>
        {t('app.car.newCar.title')}
      </Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        {t('app.car.newCar.description')}
      </Typography>

      <Box component="form" sx={{ mt: 3 }}>
        <Stack spacing={3}>
          <TextField
            placeholder={t('app.car.newUpdateCar.name')}
            label={t('app.car.newUpdateCar.name')}
            value={name}
            onBlur={handleValidate}
            onChange={(e) => setName(e.target.value)}
            fullWidth
            error={!!errors.name}
            helperText={errors.name}
          />
          <NumericFormat
            placeholder={t('app.car.newUpdateCar.year')}
            label={t('app.car.newUpdateCar.year')}
            value={year}
            onBlur={handleValidate}
            customInput={TextField}
            onValueChange={(values) => {
              if (values.floatValue) {
                setYear(values.floatValue);
              }
            }}
            error={!!errors.year}
            helperText={errors.year}
          />
          <NumericFormat
            placeholder={t('app.car.newUpdateCar.odometer')}
            label={t('app.car.newUpdateCar.odometer')}
            value={km}
            prefix="KM "
            thousandSeparator
            onBlur={handleValidate}
            customInput={TextField}
            onValueChange={(values) => {
              if (values.floatValue) {
                setKm(values.floatValue);
              }
            }}
            error={!!errors.km}
            helperText={errors.km}
          />

          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button variant="outlined" onClick={handleCancel}>
              {t('app.button.cancel')}
            </Button>
            <Button variant="contained" onClick={handleSave}>
              {t('app.button.save')}
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Container>
  );
}
