import { Grid, Stack } from '@mui/material';
import PaymentsRounded from '@mui/icons-material/PaymentsRounded';
import SpeedRounded from '@mui/icons-material/SpeedRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import RouteRounded from '@mui/icons-material/RouteRounded';
import InsightsRounded from '@mui/icons-material/InsightsRounded';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getDashboardStatsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { hasValue, useFormat } from '@/utils/format';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import StatTile from '@/components/ui/StatTile';
import EmptyState from '@/components/ui/EmptyState';
import { RouterButton } from '@/components/ui/links';
import DashboardFilter, {
  DashboardSearch,
} from '@/components/dashboard/DashboardFilter';
import CostSplit from '@/components/dashboard/CostSplit';
import AverageList from '@/components/dashboard/AverageList';
import RangeList from '@/components/dashboard/RangeList';
import ErrorPage from '@/pages/ErrorPage';

export interface DashboardPageProps {
  search: DashboardSearch;
  onSearchChange: (search: DashboardSearch) => void;
}

/** the stats endpoint uses -1 for "all cars" */
const ALL_CARS = -1;

export default function DashboardPage({
  search,
  onSearchChange,
}: DashboardPageProps) {
  const { t } = useTranslation();
  const format = useFormat();

  const { data, isLoading, isError, error } = useQuery(
    getDashboardStatsOptions({
      client: localClient,
      path: { carId: search.car ?? ALL_CARS },
      query: { from: search.from, to: search.to },
    }),
  );

  if (isError) {
    return <ErrorPage error={error} />;
  }

  const total = data?.total ?? {};
  const average = data?.average ?? {};
  const isEmpty = !isLoading && !hasValue(total.total);

  return (
    <Page id="DashboardPage">
      <PageHeader
        title={t('dashboard.title')}
        subtitle={t('dashboard.subtitle')}
      />
      <DashboardFilter search={search} onChange={onSearchChange} />

      <Grid container spacing={2} sx={{ mb: 2 }}>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatTile
            emphasis
            loading={isLoading}
            icon={<PaymentsRounded />}
            label={t('dashboard.kpi.total')}
            value={format.money(total.total)}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatTile
            emphasis
            loading={isLoading}
            icon={<SpeedRounded />}
            label={t('dashboard.kpi.costPerKm')}
            value={format.unit(average.costPerKm, 'ct/km')}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatTile
            emphasis
            loading={isLoading}
            color="fuel"
            icon={<LocalGasStationRounded />}
            label={t('dashboard.kpi.consumption')}
            value={format.unit(average.fuelConsumption, 'l/100km')}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatTile
            emphasis
            loading={isLoading}
            icon={<RouteRounded />}
            label={t('dashboard.kpi.distance')}
            value={format.unit(total.trackedDistance, 'km', 0)}
            hint={t('dashboard.kpi.fuelVolume', {
              value: format.unit(total.unit, 'l', 1),
            })}
          />
        </Grid>
      </Grid>

      {isEmpty ? (
        <EmptyState
          icon={<InsightsRounded />}
          title={t('dashboard.empty.title')}
          text={t('dashboard.empty.text')}
          action={
            <RouterButton to="/car" variant="contained">
              {t('dashboard.empty.action')}
            </RouterButton>
          }
        />
      ) : (
        data && (
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, lg: 7 }}>
              <Stack spacing={2}>
                <CostSplit total={total} />
                <AverageList average={average} />
              </Stack>
            </Grid>
            <Grid size={{ xs: 12, lg: 5 }}>
              <RangeList hiLo={data.hiLo ?? {}} />
            </Grid>
          </Grid>
        )
      )}
    </Page>
  );
}
