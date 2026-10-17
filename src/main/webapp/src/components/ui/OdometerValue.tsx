import { Box, Tooltip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { Odometer } from '@/generated';
import { useFormat } from '@/utils/format';

/**
 * The current odometer, an estimated one explains how it is calculated
 */
export default function OdometerValue({ odometer }: { odometer?: Odometer }) {
  const { t } = useTranslation();
  const format = useFormat();
  if (!odometer) return null;

  const value = format.distance(odometer.value);
  if (!odometer.driven) return <>{value}</>;

  return (
    <Tooltip
      title={t('car.odometerEstimated', {
        reading: format.distance(odometer.reading),
        date: format.date(odometer.readingDate),
        driven: format.distance(odometer.driven),
      })}
    >
      <Box
        component="span"
        tabIndex={0}
        sx={{ borderBottom: 1, borderBottomStyle: 'dotted', cursor: 'help' }}
      >
        {value}
      </Box>
    </Tooltip>
  );
}
