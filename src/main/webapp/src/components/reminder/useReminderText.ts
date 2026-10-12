import { useTranslation } from 'react-i18next';
import { Reminder } from '@/generated';
import { useFormat } from '@/utils/format';

/** Texts for when a reminder is due and how it repeats. Missing values are null in the JSON. */
export default function useReminderText() {
  const { t } = useTranslation();
  const format = useFormat();
  const km = (value: number) => format.unit(value, 'km', 0);

  const dateText = (reminder: Reminder) => {
    if (!reminder.dueDate || reminder.daysLeft == null) return undefined;
    const days = reminder.daysLeft;
    const relative =
      days < 0
        ? t('reminder.due.daysOverdue', { count: -days })
        : days === 0
          ? t('reminder.due.today')
          : t('reminder.due.inDays', { count: days });
    return `${format.date(reminder.dueDate)} · ${relative}`;
  };

  const odometerText = (reminder: Reminder) => {
    if (reminder.dueOdometer == null || reminder.kmLeft == null)
      return undefined;
    const left = reminder.kmLeft;
    const relative =
      left <= 0
        ? t('reminder.due.kmOverdue', { km: km(-left) })
        : t('reminder.due.kmLeft', { km: km(left) });
    return `${t('reminder.due.at', { km: km(reminder.dueOdometer) })} · ${relative}`;
  };

  const repeatText = (reminder: Reminder) => {
    const parts = [
      reminder.intervalMonths &&
        t('reminder.repeat.months', { count: reminder.intervalMonths }),
      reminder.intervalKm && km(reminder.intervalKm),
    ].filter(Boolean);
    return parts.length
      ? t('reminder.repeat.every', { interval: parts.join(t('reminder.or')) })
      : undefined;
  };

  return { dateText, odometerText, repeatText };
}
