import { Button, MenuItem, Stack, TextField } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import DashboardDateRange from '@/components/dashboard/DashboardDateRange';

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
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));

  return (
    <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} mb={3}>
      <TextField
        select
        label="Car"
        value={selectedCarId}
        onChange={(e) =>
          onSelectCar(e.target.value === 'all' ? -1 : Number(e.target.value))
        }
        sx={{ minWidth: 200 }}
      >
        <MenuItem value="-1">All Cars</MenuItem>
        {cars?.map((c) => (
          <MenuItem key={c.id} value={c.id}>
            {c.name}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        label="From"
        type="date"
        slotProps={{
          inputLabel: { shrink: true },
        }}
        value={dateRange.from || ''}
        onChange={(e) =>
          onChangeDateRange({ ...dateRange, from: e.target.value })
        }
      />
      <TextField
        label="To"
        type="date"
        slotProps={{
          inputLabel: { shrink: true },
        }}
        value={dateRange.to || ''}
        onChange={(e) =>
          onChangeDateRange({ ...dateRange, to: e.target.value })
        }
      />
      <Button variant="outlined">Refresh</Button>
    </Stack>
  );
}
