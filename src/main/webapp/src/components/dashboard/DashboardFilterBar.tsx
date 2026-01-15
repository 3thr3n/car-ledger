import { Button, MenuItem, Select, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import DashboardDateRange from '@/components/dashboard/DashboardDateRange';
import { DatePicker } from '@mui/x-date-pickers';
import { useTranslation } from 'react-i18next';

export interface DashboardFilterBarProps {
  selectedCarId: number;
  onSelectCar: (carId: number) => void;
  dateRange: DashboardDateRange;
  onChangeDateRange: (dateRange: DashboardDateRange) => void;
}

export default function DashboardFilterBar({
  selectedCarId,
  onSelectCar,
  dateRange,
  onChangeDateRange,
}: DashboardFilterBarProps) {
  const { t } = useTranslation();
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));

  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={3}>
      <Select
        value={selectedCarId}
        onChange={(e) =>
          onSelectCar(e.target.name === 'all' ? -1 : Number(e.target.value))
        }
        sx={{ minWidth: 200 }}
      >
        <MenuItem value="-1">All Cars</MenuItem>
        {cars?.map((c) => (
          <MenuItem key={c.id} value={c.id}>
            {c.name}
          </MenuItem>
        ))}
      </Select>

      <DatePicker
        label="From"
        name="date"
        value={dateRange.from}
        onChange={(e) => onChangeDateRange({ ...dateRange, from: e })}
      />
      <DatePicker
        label="To"
        name="date"
        value={dateRange.to}
        onChange={(e) => onChangeDateRange({ ...dateRange, to: e })}
      />
      <Button variant="outlined">{t('app.button.refresh')}</Button>
    </Stack>
  );
}
