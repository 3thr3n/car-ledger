import { Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { ReminderStatus } from '@/generated';

const colors = {
  [ReminderStatus.OVERDUE]: 'error',
  [ReminderStatus.DUE_SOON]: 'warning',
  [ReminderStatus.UPCOMING]: 'default',
} as const;

export default function ReminderStatusChip({
  status,
}: {
  status: ReminderStatus;
}) {
  const { t } = useTranslation();
  return (
    <Chip
      size="small"
      color={colors[status]}
      variant={status === ReminderStatus.UPCOMING ? 'outlined' : 'filled'}
      label={t(`reminder.status.${status}`)}
      sx={{ fontWeight: 600 }}
    />
  );
}
