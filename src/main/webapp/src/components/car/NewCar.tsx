import { Add } from '@mui/icons-material';
import { Box, Tooltip, Typography } from '@mui/material';
import React, { useEffect, useState } from 'react';
import useCarStore from '@/store/CarStore';
import useUserStore from '@/store/UserStore';
import { CarGrid } from './CarGrid';
import { useNavigate } from '@tanstack/react-router';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';
import { useTranslation } from 'react-i18next';

export default function NewCar({ index }: { index: number }) {
  const { t } = useTranslation();
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
      <CarLedgerAnimatedCard index={index} maxWidth={400}>
        <CarGrid>
          <Box
            sx={{
              display: 'flex',
              height: '224px',
              width: '100%',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Typography align="center" m={2}>
              {t('app.car.maxCars')}
            </Typography>
          </Box>
        </CarGrid>
      </CarLedgerAnimatedCard>
    );
  }

  return (
    <CarLedgerAnimatedCard index={index} maxWidth={400}>
      <CarGrid click={handleOpen}>
        <Tooltip title={tooltip} placement="top">
          <Box
            sx={{
              display: 'flex',
              height: '224px',
              width: '100%',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <Add sx={{ mr: 1 }} />{' '}
            <Typography>{t('app.car.newCar.button')}</Typography>
          </Box>
        </Tooltip>
      </CarGrid>
    </CarLedgerAnimatedCard>
  );
}
