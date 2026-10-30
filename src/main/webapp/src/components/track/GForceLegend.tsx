import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import ScaleLegend from './ScaleLegend';
import {
  gforceGradient,
  HARSH_GPS,
  HARSH_IMU,
  NO_GFORCE_COLOR,
} from './gforceScale';

/**
 * The g-force scale of the track: the same colors for both sources, each
 * harsh from its own value
 */
export default function GForceLegend() {
  const { t } = useTranslation();
  const g = (value: number) => `${value.toFixed(2)} g`;

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={{ xs: 1, sm: 3 }}
      sx={{ flexGrow: 1, alignItems: { sm: 'center' } }}
      data-testid="gforce-legend"
    >
      <ScaleLegend
        title={t('track.gforce.title')}
        label={t('track.gforce.legendLabel', {
          imu: g(HARSH_IMU),
          gps: g(HARSH_GPS),
        })}
        gradient={gforceGradient}
        ticks={[
          { at: 0, label: t('track.gforce.calm') },
          { at: 1, label: t('track.gforce.harsh') },
        ]}
      >
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {t('track.gforce.hint', { imu: g(HARSH_IMU), gps: g(HARSH_GPS) })}
        </Typography>
      </ScaleLegend>
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Box
          sx={{
            width: 24,
            height: 4,
            borderRadius: 2,
            bgcolor: NO_GFORCE_COLOR,
          }}
        />
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {t('track.gforce.none')}
        </Typography>
      </Stack>
    </Stack>
  );
}
