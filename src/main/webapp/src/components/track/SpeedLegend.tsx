import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { useFormat } from '@/utils/format';
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
      sx={{ alignItems: { sm: 'center' } }}
      data-testid="speed-legend"
    >
      <Box sx={{ flexGrow: 1, maxWidth: 360 }}>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {t('track.speed', { unit })}
        </Typography>
        <Box
          role="img"
          aria-label={t('track.legendLabel', {
            from: 0,
            to: Math.round(units.distance.fromKm(MAX_SPEED)),
            unit,
          })}
          sx={{
            height: 8,
            borderRadius: 4,
            background: legendGradient,
            mt: 0.5,
          }}
        />
        <Box sx={{ position: 'relative', height: 18 }}>
          {LEGEND_TICKS.map((kmh) => (
            <Typography
              key={kmh}
              variant="caption"
              sx={{
                position: 'absolute',
                left: `${(kmh / MAX_SPEED) * 100}%`,
                transform:
                  kmh === 0
                    ? 'none'
                    : kmh === MAX_SPEED
                      ? 'translateX(-100%)'
                      : 'translateX(-50%)',
                color: 'text.secondary',
              }}
            >
              {Math.round(units.distance.fromKm(kmh))}
            </Typography>
          ))}
        </Box>
      </Box>
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
