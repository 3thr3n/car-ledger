import { Box, Card, MenuItem, Select, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';

export interface YearSelectionProps {
  years: number[];
  selectedYear: number;
  setSelectedYear: (toSelect: number) => void;
  isMobile?: boolean;
}

export default function YearSelection({
  years,
  selectedYear,
  setSelectedYear,
  isMobile,
}: YearSelectionProps) {
  const { t } = useTranslation();

  if (isMobile) {
    return (
      <Select
        fullWidth
        value={selectedYear}
        onChange={(e) => setSelectedYear(Number(e.target.value))}
        sx={{ mb: 2 }}
      >
        <MenuItem value={-1}>{t('app.car.filter.all')}</MenuItem>
        {years.map((y) => (
          <MenuItem key={y} value={y}>
            {y}
          </MenuItem>
        ))}
      </Select>
    );
  }

  return (
    <Box sx={{ minWidth: 140 }}>
      <Typography variant="subtitle1" gutterBottom>
        {t('app.car.filter.year')}
      </Typography>
      <Stack spacing={1}>
        <Card
          sx={{
            p: 1,
            textAlign: 'center',
            cursor: 'pointer',
            bgcolor: -1 === selectedYear ? 'primary.main' : 'background.paper',
            color: -1 === selectedYear ? 'black' : 'text.primary',
            transition: '0.2s',
          }}
          onClick={() => setSelectedYear(-1)}
        >
          <Typography variant="body2">{t('app.car.filter.all')}</Typography>
        </Card>
        {years.map((y) => (
          <Card
            key={y}
            sx={{
              p: 1,
              textAlign: 'center',
              cursor: 'pointer',
              bgcolor: y === selectedYear ? 'primary.main' : 'background.paper',
              color: y === selectedYear ? 'black' : 'text.primary',
              transition: '0.2s',
            }}
            onClick={() => setSelectedYear(y)}
          >
            <Typography variant="body2">{y}</Typography>
          </Card>
        ))}
      </Stack>
    </Box>
  );
}
