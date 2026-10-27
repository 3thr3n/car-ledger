import { useTranslation } from 'react-i18next';
import { useFormat } from '@/utils/format';
import { firstDay, lastDay, Period } from './period';

/**
 * @return the days of the access as text, empty without a start and an end
 */
export default function usePeriodText() {
  const { t } = useTranslation();
  const format = useFormat();
  return (period: Period) => {
    const from = firstDay(period);
    const until = lastDay(period);
    const day = (value: NonNullable<typeof from>) =>
      format.date(value.toISOString());
    if (from && until) {
      return t('access.period.between', { from: day(from), until: day(until) });
    }
    if (from) return t('access.period.from', { from: day(from) });
    if (until) return t('access.period.until', { until: day(until) });
    return '';
  };
}
