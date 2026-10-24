import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { frequencyGradient, scaleMax } from './frequencyScale';

/** the scale of the passes, from once to the most in the period */
export default function FrequencyLegend({ maxPasses }: { maxPasses: number }) {
  const { t } = useTranslation();
  const max = scaleMax(maxPasses);

  return (
    <Box sx={{ flexGrow: 1, maxWidth: 360 }} data-testid="frequency-legend">
      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        {t('track.frequency')}
      </Typography>
      <Box
        role="img"
        aria-label={t('track.frequencyLabel', { to: max })}
        sx={{
          height: 8,
          borderRadius: 4,
          background: frequencyGradient,
          mt: 0.5,
        }}
      />
      <Stack direction="row" sx={{ justifyContent: 'space-between' }}>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          1×
        </Typography>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {max}×
        </Typography>
      </Stack>
      <Typography
        variant="caption"
        component="p"
        sx={{ color: 'text.secondary' }}
      >
        {t('track.frequencyHint')}
      </Typography>
    </Box>
  );
}
