import { Grid } from '@mui/material';
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
import EmptyState from '@/components/ui/EmptyState';
import { RouterButton } from '@/components/ui/links';
import DashboardFilter, {
  DashboardSearch,
} from '@/components/dashboard/DashboardFilter';
import PeriodSummary from '@/components/dashboard/PeriodSummary';
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

  return (
    <Page id="DashboardPage">
      <PageHeader
        title={t('dashboard.title')}
        subtitle={t('dashboard.subtitle')}
      />
      <InstallHint />
      <DashboardFilter search={search} onChange={onSearchChange} />

      {!isEmpty && (
        <PeriodSummary total={total} average={average} loading={isLoading} />
      )}

      {!!dueReminders?.length && (
        <Section
          title={t('dashboard.reminders.title')}
          subtitle={t('dashboard.reminders.subtitle', {
            context: format.units.distance.context,
          })}
          sx={{ mb: 3 }}
        >
          <ReminderList compact showCar reminders={dueReminders} />
        </Section>
      )}

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
            <Grid size={{ xs: 12, lg: 5 }}>
              <AverageList average={average} />
            </Grid>
            <Grid size={{ xs: 12, lg: 7 }}>
              <RangeList hiLo={data.hiLo ?? {}} />
            </Grid>
          </Grid>
        )
      )}
    </Page>
  );
}
