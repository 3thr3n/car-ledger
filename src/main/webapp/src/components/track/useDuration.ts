import { useTranslation } from 'react-i18next';
import { hoursOf } from './drives';

/** minutes as 25 min or 2 h 5 min */
export default function useDuration() {
  const { t } = useTranslation();
  return (total: number) =>
    total < 60
      ? t('track.drives.minutes', { count: total })
      : t('track.drives.hours', hoursOf(total));
}
