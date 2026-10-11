import { Chip, Stack } from '@mui/material';
import { useTranslation } from 'react-i18next';

export interface YearFilterProps {
  years: number[];
  value?: number;
  onChange: (year: number | undefined) => void;
}

/**
 * Year chips, based on the years the API reports for a list
 */
export default function YearFilter({
  years,
  value,
  onChange,
}: YearFilterProps) {
  const { t } = useTranslation();

  if (years.length === 0) return null;

  return (
    <Stack
      direction="row"
      spacing={1}
      sx={{ overflowX: 'auto', pb: 0.5, scrollbarWidth: 'thin' }}
    >
      <Chip
        label={t('common.allYears')}
        color={value === undefined ? 'primary' : 'default'}
        variant={value === undefined ? 'filled' : 'outlined'}
        onClick={() => onChange(undefined)}
      />
      {years.map((year) => (
        <Chip
          key={year}
          label={year}
          color={value === year ? 'primary' : 'default'}
          variant={value === year ? 'filled' : 'outlined'}
          onClick={() => onChange(year)}
        />
      ))}
    </Stack>
  );
}
