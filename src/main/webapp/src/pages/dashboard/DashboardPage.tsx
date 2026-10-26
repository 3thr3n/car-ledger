import { Grid, Stack } from '@mui/material';
import PaymentsRounded from '@mui/icons-material/PaymentsRounded';
import SpeedRounded from '@mui/icons-material/SpeedRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import EvStationRounded from '@mui/icons-material/EvStationRounded';
import RouteRounded from '@mui/icons-material/RouteRounded';
import InsightsRounded from '@mui/icons-material/InsightsRounded';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import {
  getDashboardStatsOptions,
  getMyCarsOptions,
  getRemindersOptions,
} from '@/generated/@tanstack/react-query.gen';
import { can } from '@/utils/carRole';
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
import MonthlyChart from '@/components/dashboard/LazyMonthlyChart';
import Section from '@/components/ui/Section';
import ReminderList from '@/components/reminder/ReminderList';
import InstallHint from '@/components/install/InstallHint';
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

  // due reminders are shown independent of the period
  const { data: dueReminders } = useQuery(
    getRemindersOptions({
      client: localClient,
      path: { carId: search.car ?? ALL_CARS },
      query: { onlyDue: true },
    }),
  );

  // the energy types of the shown cars, loaded by the filter already
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));
  const energyTypes = cars
    ?.filter((car) => can(car, 'READ'))
    .filter((car) => search.car === undefined || car.id === search.car)
    .map((car) => car.energyType);

  // the data of the last visit stays when the connection fails
  if (isError && !data) {
    return <ErrorPage error={error} />;
  }

  const total = data?.total ?? {};
  const average = data?.average ?? {};
  const isEmpty = !isLoading && !hasValue(total.total);
  // only charges: the consumption is in kWh, with both kWh is the hint
  const onlyCharged =
    hasValue(average.chargingConsumption) && !hasValue(average.fuelConsumption);
  const fuelVolume = format.volume(total.unit, 1);
  const chargedVolume = format.unit(total.chargedUnit, 'kWh', 0);

  return (
    <Page id="DashboardPage">
      <PageHeader
        title={t('dashboard.title')}
        subtitle={t('dashboard.subtitle')}
      />
      <InstallHint />
      <DashboardFilter search={search} onChange={onSearchChange} />

      {!!dueReminders?.length && (
        <Section
          title={t('dashboard.reminders.title')}
          subtitle={t('dashboard.reminders.subtitle', {
            context: format.units.distance.context,
          })}
          sx={{ mb: 2 }}
        >
          <ReminderList compact showCar reminders={dueReminders} />
        </Section>
      )}

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
            label={t('dashboard.kpi.costPerKm', {
              context: format.units.distance.context,
            })}
            value={format.costPerDistance(average.costPerKm)}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          {onlyCharged ? (
            <StatTile
              emphasis
              loading={isLoading}
              color="charging"
              icon={<EvStationRounded />}
              label={t('dashboard.kpi.consumption')}
              value={format.chargingConsumption(average.chargingConsumption)}
            />
          ) : (
            <StatTile
              emphasis
              loading={isLoading}
              color="fuel"
              icon={<LocalGasStationRounded />}
              label={t('dashboard.kpi.consumption')}
              value={format.consumption(average.fuelConsumption)}
              hint={
                hasValue(average.chargingConsumption)
                  ? t('dashboard.kpi.chargingConsumption', {
                      value: format.chargingConsumption(
                        average.chargingConsumption,
                      ),
                    })
                  : undefined
              }
            />
          )}
        </Grid>
        <Grid size={{ xs: 12, sm: 6, lg: 3 }}>
          <StatTile
            emphasis
            loading={isLoading}
            icon={<RouteRounded />}
            label={t('dashboard.kpi.distance')}
            value={format.distance(total.trackedDistance)}
            hint={
              hasValue(total.chargedUnit)
                ? hasValue(total.unit)
                  ? t('dashboard.kpi.bothVolumes', {
                      fuel: fuelVolume,
                      charged: chargedVolume,
                    })
                  : t('dashboard.kpi.chargedVolume', { value: chargedVolume })
                : t('dashboard.kpi.fuelVolume', { value: fuelVolume })
            }
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
            <Grid size={12}>
              <MonthlyChart
                carId={search.car ?? ALL_CARS}
                from={search.from}
                to={search.to}
                energyTypes={energyTypes}
              />
            </Grid>
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
