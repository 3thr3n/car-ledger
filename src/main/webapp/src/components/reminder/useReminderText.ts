import { useTranslation } from 'react-i18next';
import { Reminder } from '@/generated';
import { useFormat } from '@/utils/format';

/** Texts for when a reminder is due and how it repeats. Missing values are null in the JSON. */
export default function useReminderText() {
  const { t } = useTranslation();
  const format = useFormat();
  const km = (value: number) => format.distance(value);

  /** when by the date: the date and how far it is from today */
  const dateParts = (reminder: Reminder) => {
    if (!reminder.dueDate || reminder.daysLeft == null) return undefined;
    const days = reminder.daysLeft;
    return {
      absolute: format.date(reminder.dueDate),
      relative:
        days < 0
          ? t('reminder.due.daysOverdue', { count: -days })
          : days === 0
            ? t('reminder.due.today')
            : t('reminder.due.inDays', { count: days }),
    };
  };

  /** when by the mileage: the odometer and how far it is from now */
  const odometerParts = (reminder: Reminder) => {
    if (reminder.dueOdometer == null || reminder.kmLeft == null)
      return undefined;
    const left = reminder.kmLeft;
    return {
      absolute: t('reminder.due.at', { km: km(reminder.dueOdometer) }),
      relative:
        left <= 0
          ? t('reminder.due.kmOverdue', { km: km(-left) })
          : t('reminder.due.kmLeft', { km: km(left) }),
    };
  };

  const joined = (parts?: { absolute: string; relative: string }) =>
    parts && `${parts.absolute} · ${parts.relative}`;
  const dateText = (reminder: Reminder) => joined(dateParts(reminder));
  const odometerText = (reminder: Reminder) => joined(odometerParts(reminder));

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

  return { dateText, odometerText, dateParts, odometerParts, repeatText };
}
