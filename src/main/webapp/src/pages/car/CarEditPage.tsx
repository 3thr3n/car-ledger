import React, { useState } from 'react';
import {
  Box,
  Button,
  Container,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';

const getCarById = (id: string) => ({
  id,
  name: 'Toyota Corolla',
  year: 2018,
  km: 54000,
});

export interface CarEditPageProperties {
  id: string;
  navigate: (path: NavigateOptions) => void;
}

export default function CarEditPage({ id, navigate }: CarEditPageProperties) {
  const car = getCarById(id!);

  const [name, setName] = useState(car.name);
  const [year, setYear] = useState(car.year);
  const [km, setKm] = useState(car.km);

  const handleSave = () => {
    // TODO: persist changes via backend API
    console.log({ id, name, year, km });
    navigate({ to: '/car/$id', params: { id } }); // navigate back to car
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
