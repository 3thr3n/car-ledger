import { NavigateOptions } from '@tanstack/router-core';
import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { NumericFormat } from 'react-number-format';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  getMyCarOptions,
  updateMyCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import { useTranslation } from 'react-i18next';
import CarLedgerPage from '@/components/CarLedgerPage';
import { createCar, CreateCarData, Options } from '@/generated';

export interface CarNewPageProperties {
  navigate: (path: NavigateOptions) => void;
  id?: string;
}

export default function CarUpdatePage({ navigate, id }: CarNewPageProperties) {
  const { t } = useTranslation();

  const currentYear = new Date().getFullYear();

  const [name, setName] = useState('');
  const [year, setYear] = useState<number | undefined>(currentYear);
  const [km, setKm] = useState<number | undefined>(0);
  const [errors, setErrors] = useState<{
    name?: string;
    year?: string;
    km?: string;
  }>({});

  const {
    data: car,
    isLoading,
    isError,
  } = useQuery({
    ...getMyCarOptions({
      path: {
        id: Number(id),
      },
      client: localClient,
    }),
    enabled: !!id,
  });

  useEffect(() => {
    if (!car) return;

    setName(car.name ?? '');
    setYear(car.year ?? currentYear);
    setKm(car.odometer ?? 0);
  }, [car, currentYear, setKm, setName, setYear]);

  const { mutate: mutateNewCar } = useMutation({
    mutationFn: async (fnOptions: Options<CreateCarData>) => {
      const { response } = await createCar({
        client: localClient,
        redirect: 'follow',
        ...fnOptions,
        throwOnError: true,
      });
      return response;
    },
    onSuccess: (response) => {
      const location = response.headers.get('Location');
      toast.info('Car saved!');
      if (location) {
        const id = location?.split('/').pop();
        navigate({ to: '/car/$id', params: { id }, replace: true });
      }
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

  const { mutate: mutateUpdateCar } = useMutation({
    ...updateMyCarMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Car updated!');
      navigate({ to: '..', params: { id: id! }, replace: true });
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
    if (km == undefined || km < 0) {
      newErrors.km = 'Odometer must be a positive number';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSave = () => {
    if (!handleValidate()) return;

    if (id) {
      mutateUpdateCar({
        path: { id: Number(id) },
        body: { name, year, odometer: km },
      });
    } else {
      mutateNewCar({ body: { name, year, odometer: km } });
    }
  };

  const handleCancel = () => {
    if (id) {
      navigate({ to: '/car/$id', params: { id } });
    } else {
      navigate({ to: '/car' });
    }
  };

  if (isLoading) {
    return <></>;
  }

  if (isError) {
    return <></>;
  }

  return (
    <CarLedgerPage id="CarUpdatePage">
      <Container maxWidth="sm">
        <Typography variant="h4" gutterBottom>
          {id ? t('app.car.updateCar.title') : t('app.car.newCar.title')}
        </Typography>
        <Typography variant="body1" color="text.secondary" gutterBottom>
          {id
            ? t('app.car.updateCar.description')
            : t('app.car.newCar.description')}
        </Typography>

        <Box component="form" sx={{ mt: 3 }}>
          <Stack spacing={3}>
            <TextField
              placeholder={t('app.car.common.model')}
              label={t('app.car.common.model')}
              value={name}
              onBlur={handleValidate}
              onChange={(e) => setName(e.target.value)}
              fullWidth
              error={!!errors.name}
              helperText={errors.name}
            />
            <NumericFormat
              placeholder={t('app.car.common.year')}
              label={t('app.car.common.year')}
              value={year}
              onBlur={handleValidate}
              customInput={TextField}
              onValueChange={(values) => {
                setYear(values.floatValue);
              }}
              error={!!errors.year}
              helperText={errors.year}
            />
            <NumericFormat
              placeholder={t('app.car.common.odometer')}
              label={t('app.car.common.odometer')}
              value={km}
              prefix="KM "
              thousandSeparator
              onBlur={handleValidate}
              customInput={TextField}
              onValueChange={(values) => {
                setKm(values.floatValue);
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
    </CarLedgerPage>
  );
}
