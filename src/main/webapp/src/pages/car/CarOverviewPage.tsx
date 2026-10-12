import { Box, Divider, Grid, Stack, Typography } from '@mui/material';
import PaymentsRounded from '@mui/icons-material/PaymentsRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import BuildRounded from '@mui/icons-material/BuildRounded';
import ReceiptLongRounded from '@mui/icons-material/ReceiptLongRounded';
import WaterDropRounded from '@mui/icons-material/WaterDropRounded';
import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { Fragment } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import {
  getAllRecurringBillsOptions,
  getMyCarOverviewOptions,
  getRemindersOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { BillType } from '@/generated';
import { toApiDate, useFormat } from '@/utils/format';
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

  const ledger = useBillList('all', id, 10);
  const { columns, mobileRow } = useLedgerColumns();
  const onDelete = useDeleteBill(id);
  const navigate = useNavigate();

  const { data: running } = useQuery(
    getAllRecurringBillsOptions({
      client: localClient,
      path: { carId: id },
      query: { onlyRunning: true, page: 1, size: 5 },
    }),
  );

  const { data: reminders } = useQuery(
    getRemindersOptions({ client: localClient, path: { carId: id } }),
  );

  const tiles = [
    {
      key: 'totalCost',
      icon: <PaymentsRounded />,
      color: 'primary',
      value: format.money(overview?.totalCost),
    },
    {
      key: 'avgConsumption',
      icon: <WaterDropRounded />,
      color: 'fuel',
      value: format.unit(overview?.avgConsumption, 'l/100km'),
    },
    {
      key: 'refuels',
      icon: <LocalGasStationRounded />,
      color: 'fuel',
      value: overview?.totalRefuels ?? 0,
    },
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
            size={{ xs: index === 0 ? 12 : 6, sm: 4, lg: 'grow' }}
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
        subtitle={t('dashboard.filter.period.last12')}
      />

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <Section
            flush
            title={t('car.overview.ledger')}
            subtitle={t('car.overview.ledgerSubtitle')}
            action={
              <RouterButton
                to="/dashboard"
                search={{ car: id }}
                size="small"
                endIcon={<ArrowForwardRounded />}
              >
                {t('car.overview.toDashboard')}
              </RouterButton>
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
              rows={ledger.rows}
              total={ledger.total}
              columns={columns}
              mobileRow={mobileRow}
              pageModel={ledger.pageModel}
              onPageModelChange={ledger.setPageModel}
              sortModel={ledger.sortModel}
              onSortModelChange={ledger.setSortModel}
              loading={ledger.isLoading || ledger.isFetching}
              onDelete={onDelete}
              onEdit={(row) =>
                row.type &&
                row.type !== BillType.RECURRING &&
                navigate({
                  to: '/car/$id/bill/$billId/edit',
                  params: { id: `${id}`, billId: `${row.id}` },
                  search: { type: row.type },
                })
              }
              emptyTitle={t('car.overview.emptyTitle')}
              emptyText={t('car.overview.emptyText')}
              emptyAction={
                <RouterButton
                  to="/car/$id/bill/add"
                  params={params}
                  variant="contained"
                >
                  {t('bill.add.button')}
                </RouterButton>
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
                    <RouterButton
                      to="/car/$id/reminders"
                      params={params}
                      size="small"
                      variant="outlined"
                    >
                      {t('reminder.add.button')}
                    </RouterButton>
                  }
                />
              )}
            </Section>
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
                        alignItems="center"
                        sx={{ py: 1.25 }}
                      >
                        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                          <Typography fontWeight={600} noWrap>
                            {bill.name}
                          </Typography>
                          <Typography variant="body2" color="text.secondary">
                            {t('car.overview.nextDue', {
                              date: format.date(bill.nextDueDate),
                            })}
                          </Typography>
                        </Box>
                        <Typography fontWeight={600} className="tabular">
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
                    <RouterButton
                      to="/car/$id/bill/add"
                      params={params}
                      search={{ type: BillType.RECURRING }}
                      size="small"
                      variant="outlined"
                    >
                      {t('car.overview.addRecurring')}
                    </RouterButton>
                  }
                />
              )}
            </Section>
          </Stack>
        </Grid>
      </Grid>
    </Stack>
  );
}
