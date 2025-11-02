import { Add } from '@mui/icons-material';
import { Box, Tooltip, Typography } from '@mui/material';
import React, { useEffect, useState } from 'react';
import useCarStore from '@/store/CarStore';
import useUserStore from '@/store/UserStore';
import { CarGrid, CarGridContent } from '@/components/carGrid/CarGrid';
import { useNavigate } from '@tanstack/react-router';

export default function NewCar() {
  const navigate = useNavigate();
  const [disabled, setDisabled] = useState(true);

  const maxCars = useUserStore((state) => state.maxCars);
  const currentCarSize = useCarStore((state) => state.carSize);

  const handleOpen = async () => {
    if (!disabled) {
      await navigate({ to: '/car/new' });
    }
  };

  useEffect(() => {
    if (maxCars <= currentCarSize) {
      setDisabled(true);
    } else {
      setDisabled(false);
    }
  }, [maxCars, currentCarSize]);

  const tooltip =
    'Add new Car, you can still add ' + (maxCars - currentCarSize) + ' cars';
  if (disabled) {
    return (
      <CarGrid>
        <Box
          sx={{
            display: 'flex',
            height: '100%',
            width: '100%',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <Typography align="center" mx={2}>
            Max cars reached, either delete one or ask the administrator to
            allow one more!
          </Typography>
        </Box>
      </CarGrid>
    );
  }

  return (
    <CarGrid click={handleOpen}>
      <CarGridContent>
        <Tooltip title={tooltip} placement="top">
          <Box
            sx={{
              display: 'flex',
              height: '100%',
              width: '100%',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Add sx={{ mr: 1 }} /> <Typography>New car</Typography>
          </Box>
        </Tooltip>
      </CarGridContent>
    </CarGrid>
  );
}
