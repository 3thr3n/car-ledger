import { Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import ScaleLegend from './ScaleLegend';
import { frequencyGradient, scaleMax } from './frequencyScale';

/** the scale of the passes, from once to the most in the period */
export default function FrequencyLegend({ maxPasses }: { maxPasses: number }) {
  const { t } = useTranslation();
  const max = scaleMax(maxPasses);

  return (
    <ScaleLegend
      title={t('track.frequency')}
      label={t('track.frequencyLabel', { to: max })}
      gradient={frequencyGradient}
      ticks={[
        { at: 0, label: '1×' },
        { at: 1, label: `${max}×` },
      ]}
      testId="frequency-legend"
    >
      <Typography
        variant="caption"
        component="p"
        sx={{ color: 'text.secondary' }}
      >
        {t('track.frequencyHint')}
      </Typography>
    </ScaleLegend>
  );
}
