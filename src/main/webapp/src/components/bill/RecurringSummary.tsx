import { Box, Typography } from '@mui/material';
import EventRepeatRounded from '@mui/icons-material/EventRepeatRounded';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { BillInterval, RecurringBillSort } from '@/generated';
import { getAllRecurringBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useFormat } from '@/utils/format';
import { CLUSTER_AMBER, displayFont } from '@/theme';

/** a payment due within this many days lights its lamp */
const SOON_DAYS = 14;

/** the share of a month of one payment in the interval */
const PER_MONTH: Record<BillInterval, number> = {
  [BillInterval.ONCE]: 0,
  [BillInterval.MONTHLY]: 1,
  [BillInterval.QUARTERLY]: 1 / 3,
  [BillInterval.SEMI_ANNUALLY]: 1 / 6,
  [BillInterval.ANNUALLY]: 1 / 12,
};

/**
 * What the running recurring costs come to in a month, and the next payment
 * as a warning light, lit when it is due soon
 */
export default function RecurringSummary({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const { data } = useQuery(
    getAllRecurringBillsOptions({
      client: localClient,
      path: { carId },
      query: {
        onlyRunning: true,
        sort: RecurringBillSort.NEXT_DUE,
        page: 1,
        size: 100,
      },
    }),
  );
  const running = data?.data ?? [];
  if (!running.length) return null;

  const perMonth = running.reduce(
    (sum, bill) =>
      sum +
      (bill.amount ?? 0) * PER_MONTH[bill.billInterval ?? BillInterval.ONCE],
    0,
  );
  const next = running[0];
  const soon =
    !!next.nextDueDate &&
    dayjs(next.nextDueDate).diff(dayjs().startOf('day'), 'day') <= SOON_DAYS;

  return (
    <Box
      component="section"
      aria-label={t('bill.summary.label')}
      data-testid="cost-summary"
      sx={{ px: 2.5, pt: 2.5, pb: 1 }}
    >
      <Typography
        className="tabular"
        sx={(theme) => ({
          fontFamily: displayFont,
          fontWeight: 700,
          fontSize: { xs: '2.25rem', sm: '2.75rem' },
          lineHeight: 1.05,
          ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
        })}
      >
        {format.money(perMonth)}
      </Typography>
      <Typography sx={{ color: 'text.secondary' }}>
        {t('bill.summary.perMonth', { count: running.length })}
      </Typography>
      {next.nextDueDate && (
        <Box
          sx={(theme) => ({
            mt: 1.5,
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
            color: 'text.secondary',
            ...(soon && {
              color: theme.vars.palette.warning.main,
              ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
            }),
          })}
        >
          <EventRepeatRounded aria-hidden />
          <Typography sx={{ fontWeight: soon ? 600 : 400, color: 'inherit' }}>
            {t('bill.summary.next', {
              name: next.name,
              amount: format.money(next.amount),
              date: format.date(next.nextDueDate),
            })}
          </Typography>
        </Box>
      )}
    </Box>
  );
}
