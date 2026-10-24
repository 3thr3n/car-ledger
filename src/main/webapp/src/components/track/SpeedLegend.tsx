import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useFormat } from '@/utils/format';
import ScaleLegend from './ScaleLegend';
import {
  LEGEND_TICKS,
  legendGradient,
  MAX_SPEED,
  NO_SPEED_COLOR,
} from './speedScale';

/** the speed scale of the track, in the distance unit of the account */
export default function SpeedLegend() {
  const { t } = useTranslation();
  const { units } = useFormat();
  const unit = `${units.distance.label}/h`;

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={{ xs: 1, sm: 3 }}
      sx={{ flexGrow: 1, alignItems: { sm: 'center' } }}
      data-testid="speed-legend"
    >
      <ScaleLegend
        title={t('track.speed', { unit })}
        label={t('track.legendLabel', {
          from: 0,
          to: Math.round(units.distance.fromKm(MAX_SPEED)),
          unit,
        })}
        gradient={legendGradient}
        ticks={LEGEND_TICKS.map((kmh) => ({
          at: kmh / MAX_SPEED,
          label: Math.round(units.distance.fromKm(kmh)),
        }))}
      />
      <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
        <Box
          sx={{
            width: 24,
            height: 4,
            borderRadius: 2,
            bgcolor: NO_SPEED_COLOR,
          }}
        />
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {t('track.noSpeed')}
        </Typography>
      </Stack>
    </Stack>
  );
}
