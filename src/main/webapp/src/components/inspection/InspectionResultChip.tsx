import { Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { InspectionResult } from '@/generated';

const colors = {
  [InspectionResult.NO_DEFECTS]: 'success',
  [InspectionResult.MINOR_DEFECTS]: 'success',
  [InspectionResult.MAJOR_DEFECTS]: 'warning',
  [InspectionResult.DANGEROUS_DEFECTS]: 'error',
  [InspectionResult.UNSAFE]: 'error',
} as const;

export default function InspectionResultChip({
  result,
}: {
  result: InspectionResult;
}) {
  const { t } = useTranslation();
  return (
    <Chip
      size="small"
      color={colors[result]}
      // the plaque only with no or minor defects
      variant={
        result === InspectionResult.MINOR_DEFECTS ? 'outlined' : 'filled'
      }
      label={t(`inspection.results.${result}`)}
      sx={{ fontWeight: 600 }}
    />
  );
}
