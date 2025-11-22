import React, { useEffect, useState } from 'react';
import {
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  getMyCarOptions,
  updateMyCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';

export interface CarEditPageProperties {
  id: string;
  navigate: (path: NavigateOptions) => void;
}

export default function CarEditPage({ id, navigate }: CarEditPageProperties) {
  const thisYear = new Date().getFullYear();

  const [name, setName] = useState('');
  const [year, setYear] = useState(thisYear);
  const [km, setKm] = useState(0);

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
  });

  const { mutate } = useMutation({
    ...updateMyCarMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Car updated!');
      navigate({ to: '..', params: { id } });
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

  useEffect(() => {
    if (car) {
      setName(car.name ?? '');
      setYear(car.year ?? thisYear);
      setKm(car.odometer ?? 0);
    }
  }, [car, thisYear, setKm, setName, setYear]);

  if (isLoading) {
    return <></>;
  }

  if (isError || !car) {
    return <></>;
  }

  const handleSave = () => {
    mutate({
      body: {
        name,
        year,
        odometer: km,
      },
      path: {
        id: Number(id),
      },
    });
  };

  const handleCancel = () => {
    navigate({ to: '/car/$id', params: { id } });
  };

  return (
    <Container maxWidth="sm" sx={{ py: 5 }}>
      <Typography variant="h4" gutterBottom>
        Edit Car
      </Typography>
      <Typography variant="body1" color="text.secondary" gutterBottom>
        Update your vehicle details below.
      </Typography>

      <Box component="form" sx={{ mt: 3 }}>
        <Stack spacing={3}>
          <TextField
            label="Car Name / Model"
            value={name}
            onChange={(e) => setName(e.target.value)}
            fullWidth
          />
          <TextField
            label="Year"
            type="number"
            value={year}
            onChange={(e) => setYear(Number(e.target.value))}
            fullWidth
          />
          <TextField
            label="Current Odometer (KM)"
            type="number"
            value={km}
            onChange={(e) => setKm(Number(e.target.value))}
            fullWidth
          />

          <Stack direction="row" spacing={2} justifyContent="flex-end">
            <Button variant="outlined" onClick={handleCancel}>
              Cancel
            </Button>
            <Button variant="contained" onClick={handleSave}>
              Save
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Container>
  );
}
