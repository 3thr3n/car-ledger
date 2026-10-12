import {
  MenuItem,
  Skeleton,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { lazy, Suspense, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate } from '@/utils/format';

// the date pickers are only needed for a custom period
const DateRangeFields = lazy(() => import('./DateRangeFields'));

export interface DashboardSearch {
  car?: number;
  from?: string;
  to?: string;
}

type Period = 'all' | 'year' | 'last12' | 'custom';

function periodOf(search: DashboardSearch): Period {
  if (!search.from && !search.to) return 'all';
  const today = toApiDate(dayjs());
  if (search.to === today || !search.to) {
    if (search.from === toApiDate(dayjs().startOf('year'))) return 'year';
    if (search.from === toApiDate(dayjs().subtract(12, 'month')))
      return 'last12';
  }
  return 'custom';
}

export default function DashboardFilter({
  search,
  onChange,
}: {
  search: DashboardSearch;
  onChange: (search: DashboardSearch) => void;
}) {
  const { t } = useTranslation();
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));
  const [customOpen, setCustomOpen] = useState(false);

  const period = customOpen ? 'custom' : periodOf(search);

  const selectPeriod = (value: Period | null) => {
    if (!value) return;
    setCustomOpen(value === 'custom');
    const today = toApiDate(dayjs());
    if (value === 'all')
      onChange({ ...search, from: undefined, to: undefined });
    if (value === 'year')
      onChange({
        ...search,
        from: toApiDate(dayjs().startOf('year')),
        to: today,
      });
    if (value === 'last12')
      onChange({
        ...search,
        from: toApiDate(dayjs().subtract(12, 'month')),
        to: today,
      });
  };

  return (
    <Stack
      direction={{ xs: 'column', md: 'row' }}
      spacing={1.5}
      sx={{
        alignItems: { xs: 'stretch', md: 'center' },
        mb: 3,
      }}
    >
      <TextField
        select
        size="small"
        value={search.car ?? -1}
        onChange={(e) => {
          const car = Number(e.target.value);
          onChange({ ...search, car: car === -1 ? undefined : car });
        }}
        sx={{ width: { md: 240 } }}
        label={t('dashboard.filter.car')}
      >
        <MenuItem value={-1}>{t('dashboard.filter.allCars')}</MenuItem>
        {cars?.map((car) => (
          <MenuItem key={car.id} value={car.id}>
            {car.name}
          </MenuItem>
        ))}
      </TextField>

      <ToggleButtonGroup
        size="small"
        exclusive
        value={period}
        onChange={(_, value) => selectPeriod(value)}
        sx={{ flexWrap: 'wrap' }}
      >
        {(['all', 'year', 'last12', 'custom'] as Period[]).map((value) => (
          <ToggleButton key={value} value={value} sx={{ px: 1.5 }}>
            {t(`dashboard.filter.period.${value}`)}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>

      {period === 'custom' && (
        <Suspense
          fallback={<Skeleton variant="rounded" width={330} height={40} />}
        >
          <DateRangeFields
            from={search.from}
            to={search.to}
            onChange={(range) => onChange({ ...search, ...range })}
          />
        </Suspense>
      )}
    </Stack>
  );
}
