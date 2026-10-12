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

  const value = format.unit(odometer.value, 'km', 0);
  if (!odometer.driven) return <>{value}</>;

  return (
    <Tooltip
      title={t('car.odometerEstimated', {
        reading: format.unit(odometer.reading, 'km', 0),
        date: format.date(odometer.readingDate),
        driven: format.unit(odometer.driven, 'km', 0),
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
