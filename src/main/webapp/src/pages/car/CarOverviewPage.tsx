import { Box, Button, Divider, Grid, Stack, Typography } from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import PaymentsRounded from '@mui/icons-material/PaymentsRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import BuildRounded from '@mui/icons-material/BuildRounded';
import ReceiptLongRounded from '@mui/icons-material/ReceiptLongRounded';
import WaterDropRounded from '@mui/icons-material/WaterDropRounded';
import EvStationRounded from '@mui/icons-material/EvStationRounded';
import BoltRounded from '@mui/icons-material/BoltRounded';
import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { Fragment } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import {
  getAllRecurringBillsOptions,
  getMyCarOptions,
  getMyCarOverviewOptions,
  getRemindersOptions,
} from '@/generated/@tanstack/react-query.gen';
import { baseUrl, localClient } from '@/utils/QueryClient';
import { BillType, RecurringBillSort } from '@/generated';
import { toApiDate, useFormat } from '@/utils/format';
import { hasBattery, hasTank } from '@/utils/energy';
import useBillList from '@/hooks/useBillList';
import StatTile from '@/components/ui/StatTile';
import Section from '@/components/ui/Section';
import YearFilter from '@/components/ui/YearFilter';
import EmptyState from '@/components/ui/EmptyState';
import { RouterButton } from '@/components/ui/links';
import BillTable from '@/components/bill/BillTable';
import { useLedgerColumns } from '@/components/bill/columns';
import useDeleteBill from '@/components/bill/useDeleteBill';
import MonthlyChart from '@/components/dashboard/LazyMonthlyChart';
import ReminderList from '@/components/reminder/ReminderList';
import CarDetails from '@/components/car/CarDetails';
import PapersSummary from '@/components/car/PapersSummary';
import { can } from '@/utils/carRole';

/** the chart of the overview shows the current and the 11 months before */
const lastTwelveMonths = () => ({
  from: toApiDate(dayjs().subtract(11, 'month').startOf('month')),
  to: toApiDate(dayjs().endOf('month')),
});

export default function CarOverviewPage({ id }: { id: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const params = { id: `${id}` };

  const { data: overview, isLoading } = useQuery(
    getMyCarOverviewOptions({ client: localClient, path: { id } }),
  );

  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id } }),
  );

  const ledger = useBillList('all', id, 10);
  const { columns, mobileRow } = useLedgerColumns();
  const onDelete = useDeleteBill(id);
  const navigate = useNavigate();
  const canChange = can(car, 'WRITE');

  const { data: running } = useQuery(
    getAllRecurringBillsOptions({
      client: localClient,
      path: { carId: id },
      // the next due payment on top
      query: {
        onlyRunning: true,
        sort: RecurringBillSort.NEXT_DUE,
        page: 1,
        size: 5,
      },
    }),
  );

  const { data: reminders } = useQuery(
    getRemindersOptions({ client: localClient, path: { carId: id } }),
  );

  const fuelTiles = [
    {
      key: 'avgConsumption',
      icon: <WaterDropRounded />,
      color: 'fuel',
      value: format.consumption(overview?.avgConsumption),
    },
    {
      key: 'refuels',
      icon: <LocalGasStationRounded />,
      color: 'fuel',
      value: overview?.totalRefuels ?? 0,
    },
  ] as const;
  const chargingTiles = [
    {
      key: 'avgChargingConsumption',
      icon: <BoltRounded />,
      color: 'charging',
      value: format.chargingConsumption(overview?.avgChargingConsumption),
    },
    {
      key: 'charges',
      icon: <EvStationRounded />,
      color: 'charging',
      value: overview?.totalCharges ?? 0,
    },
  ] as const;

  const tiles = [
    {
      key: 'totalCost',
      icon: <PaymentsRounded />,
      color: 'primary',
      value: format.money(overview?.totalCost),
    },
    ...(hasTank(car?.energyType) ? fuelTiles : []),
    ...(hasBattery(car?.energyType) ? chargingTiles : []),
    {
      key: 'maintenance',
      icon: <BuildRounded />,
      color: 'maintenance',
      value: overview?.totalMaintenanceEvents ?? 0,
    },
    {
      key: 'miscellaneous',
      icon: <ReceiptLongRounded />,
      color: 'miscellaneous',
      value: overview?.totalMiscellaneousEvents ?? 0,
    },
  ] as const;

  return (
    <Stack spacing={3}>
      <Grid container spacing={2}>
        {tiles.map((tile, index) => (
          <Grid
            key={tile.key}
            // a plug-in hybrid has 7 tiles, too many for one row
            size={{
              xs: index === 0 ? 12 : 6,
              sm: 4,
              lg: tiles.length > 5 ? 3 : 'grow',
            }}
          >
            <StatTile
              loading={isLoading}
              emphasis={index === 0}
              icon={tile.icon}
              color={tile.color}
              label={t(`car.overview.${tile.key}`)}
              value={tile.value}
            />
          </Grid>
        ))}
      </Grid>

      <MonthlyChart
        carId={id}
        {...lastTwelveMonths()}
        energyTypes={car && [car.energyType]}
        subtitle={t('dashboard.filter.period.last12')}
      />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Section
            flush
            title={t('car.overview.ledger')}
            subtitle={t('car.overview.ledgerSubtitle')}
            action={
              <Stack direction="row" spacing={1}>
                {/* every entry and the recurring payments, for spreadsheets */}
                {ledger.total > 0 && (
                  <Button
                    href={`${baseUrl}/api/export/${id}/all${ledger.year === undefined ? '' : `?year=${ledger.year}`}`}
                    size="small"
                    startIcon={<FileDownloadRounded />}
                  >
                    {t('car.overview.exportAll')}
                  </Button>
                )}
                <RouterButton
                  to="/dashboard"
                  search={{ car: id }}
                  size="small"
                  endIcon={<ArrowForwardRounded />}
                >
                  {t('car.overview.toDashboard')}
                </RouterButton>
              </Stack>
            }
          >
            <Box sx={{ px: 2.5, pb: 1.5 }}>
              <YearFilter
                years={ledger.years}
                value={ledger.year}
                onChange={ledger.setYear}
              />
            </Box>
            <BillTable
              carId={id}
              rows={ledger.rows}
              total={ledger.total}
              columns={columns}
              mobileRow={mobileRow}
              pageModel={ledger.pageModel}
              onPageModelChange={ledger.setPageModel}
              sortModel={ledger.sortModel}
              onSortModelChange={ledger.setSortModel}
              loading={ledger.isLoading || ledger.isFetching}
              onDelete={canChange ? onDelete : undefined}
              onEdit={
                canChange
                  ? (row) =>
                      row.type &&
                      row.type !== BillType.RECURRING &&
                      navigate({
                        to: '/car/$id/bill/$billId/edit',
                        params: { id: `${id}`, billId: `${row.id}` },
                        search: { type: row.type },
                      })
                  : undefined
              }
              emptyTitle={t('car.overview.emptyTitle')}
              emptyText={t('car.overview.emptyText')}
              emptyAction={
                canChange && (
                  <RouterButton
                    to="/car/$id/bill/add"
                    params={params}
                    variant="contained"
                  >
                    {t('bill.add.button')}
                  </RouterButton>
                )
              }
            />
          </Section>
        </Grid>

        <Grid size={{ xs: 12, lg: 4 }}>
          <Stack spacing={3}>
            <Section
              title={t('car.overview.reminders')}
              action={
                <RouterButton
                  to="/car/$id/reminders"
                  params={params}
                  size="small"
                >
                  {t('common.all')}
                </RouterButton>
              }
            >
              {reminders?.length ? (
                <ReminderList compact reminders={reminders.slice(0, 3)} />
              ) : (
                <EmptyState
                  compact
                  title={t('car.overview.remindersEmpty')}
                  action={
                    canChange && (
                      <RouterButton
                        to="/car/$id/reminders"
                        params={params}
                        size="small"
                        variant="outlined"
                      >
                        {t('reminder.add.button')}
                      </RouterButton>
                    )
                  }
                />
              )}
            </Section>
            <PapersSummary carId={id} />
            <Section
              title={t('car.overview.running')}
              action={
                <RouterButton
                  to="/car/$id/bill/recurring"
                  params={params}
                  size="small"
                >
                  {t('common.all')}
                </RouterButton>
              }
            >
              {running?.data?.length ? (
                <Stack>
                  {running.data.map((bill, index) => (
                    <Fragment key={bill.id}>
                      {index > 0 && <Divider />}
                      <Stack
                        direction="row"
                        spacing={2}
                        sx={{
                          alignItems: 'center',
                          py: 1.25,
                        }}
                      >
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Typography
                            noWrap
                            sx={{
                              fontWeight: 600,
                            }}
                          >
                            {bill.name}
                          </Typography>
                          <Typography
                            variant="body2"
                            sx={{
                              color: 'text.secondary',
                            }}
                          >
                            {t('car.overview.nextDue', {
                              date: format.date(bill.nextDueDate),
                            })}
                          </Typography>
                        </Box>
                        <Typography
                          className="tabular"
                          sx={{
                            fontWeight: 600,
                          }}
                        >
                          {format.money(bill.amount)}
                        </Typography>
                      </Stack>
                    </Fragment>
                  ))}
                </Stack>
              ) : (
                <EmptyState
                  compact
                  title={t('car.overview.runningEmpty')}
                  action={
                    canChange && (
                      <RouterButton
                        to="/car/$id/bill/add"
                        params={params}
                        search={{ type: BillType.RECURRING }}
                        size="small"
                        variant="outlined"
                      >
                        {t('car.overview.addRecurring')}
                      </RouterButton>
                    )
                  }
                />
              )}
            </Section>
            {car && <CarDetails car={car} totalCost={overview?.totalCost} />}
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}
