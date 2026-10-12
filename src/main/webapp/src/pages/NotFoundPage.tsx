import { useMemo } from 'react';
import { Stack, Typography } from '@mui/material';
import AltRouteRounded from '@mui/icons-material/AltRouteRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import BuildCircleRounded from '@mui/icons-material/BuildCircleRounded';
import TrafficRounded from '@mui/icons-material/TrafficRounded';
import { useTranslation } from 'react-i18next';
import { RouterButton } from '@/components/ui/links';

const variants = [
  { key: 'wrongTurn', icon: <AltRouteRounded /> },
  { key: 'outOfFuel', icon: <LocalGasStationRounded /> },
  { key: 'engine', icon: <BuildCircleRounded /> },
  { key: 'redLight', icon: <TrafficRounded /> },
];

export default function NotFoundPage() {
  const { t } = useTranslation();

  // Pick one message randomly on first render
  const variant = useMemo(
    () => variants[Math.floor(Math.random() * variants.length)],
    [],
  );

  return (
    <Stack
      id="NotFoundPage"
      spacing={2}
      sx={{
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        flexGrow: 1,
        py: 10,
        px: 2,
        '& > svg': { fontSize: 56, color: 'primary.main' },
      }}
    >
      {variant.icon}
      <Typography
        variant="overline"
        sx={{
          color: 'text.secondary',
        }}
      >
        404
      </Typography>
      <Typography variant="h4">{t(`notFound.${variant.key}.title`)}</Typography>
      <Typography
        sx={{
          color: 'text.secondary',
        }}
      >
        {t(`notFound.${variant.key}.text`)}
      </Typography>
      <RouterButton to="/" variant="contained">
        {t('errors.goHome')}
      </RouterButton>
    </Stack>
  );
}
