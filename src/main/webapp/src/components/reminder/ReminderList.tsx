import {
  Box,
  Divider,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import EventRounded from '@mui/icons-material/EventRounded';
import SpeedRounded from '@mui/icons-material/SpeedRounded';
import RepeatRounded from '@mui/icons-material/RepeatRounded';
import TaskAltRounded from '@mui/icons-material/TaskAltRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import { Fragment, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Reminder } from '@/generated';
import { RouterLink } from '@/components/ui/links';
import ReminderStatusChip from './ReminderStatusChip';
import useReminderText from './useReminderText';

export interface ReminderListProps {
  reminders: Reminder[];
  /** shows the car, as a link to its reminders */
  showCar?: boolean;
  /** only the next due date/odometer, without repetition and note */
  compact?: boolean;
  onDone?: (reminder: Reminder) => void;
  onEdit?: (reminder: Reminder) => void;
  onDelete?: (reminder: Reminder) => void;
}

function Detail({ icon, children }: { icon: ReactNode; children: ReactNode }) {
  return (
    <Stack
      direction="row"
      spacing={0.75}
      sx={{
        alignItems: 'center',
        color: 'text.secondary',
        '& svg': { fontSize: 16 },
      }}
    >
      {icon}
      <Typography
        variant="body2"
        sx={{
          color: 'inherit',
        }}
      >
        {children}
      </Typography>
    </Stack>
  );
}

export default function ReminderList({
  reminders,
  showCar,
  compact,
  onDone,
  onEdit,
  onDelete,
}: ReminderListProps) {
  const { t } = useTranslation();
  const text = useReminderText();

  return (
    <Stack>
      {reminders.map((reminder, index) => {
        const date = text.dateText(reminder);
        const odometer = text.odometerText(reminder);
        const repeat = !compact && text.repeatText(reminder);

        return (
          <Fragment key={reminder.id}>
            {index > 0 && <Divider />}
            <Stack
              direction="row"
              spacing={1.5}
              data-testid="reminder"
              sx={{
                alignItems: 'flex-start',
                py: 1.5,
              }}
            >
              <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                <Stack
                  direction="row"
                  spacing={1}
                  useFlexGap
                  sx={{
                    alignItems: 'center',
                    flexWrap: 'wrap',
                    mb: 0.5,
                  }}
                >
                  <Typography
                    sx={{
                      fontWeight: 600,
                    }}
                  >
                    {reminder.title}
                  </Typography>
                  <ReminderStatusChip status={reminder.status} />
                </Stack>
                {showCar && (
                  <RouterLink
                    to="/car/$id/reminders"
                    params={{ id: `${reminder.carId}` }}
                    variant="body2"
                    underline="hover"
                  >
                    {reminder.carName}
                  </RouterLink>
                )}
                <Stack spacing={0.25} sx={{ mt: 0.25 }}>
                  {date && <Detail icon={<EventRounded />}>{date}</Detail>}
                  {odometer && (
                    <Detail icon={<SpeedRounded />}>{odometer}</Detail>
                  )}
                  {repeat && <Detail icon={<RepeatRounded />}>{repeat}</Detail>}
                </Stack>
                {!compact && reminder.note && (
                  <Typography
                    variant="body2"
                    sx={{ mt: 0.75, whiteSpace: 'pre-line' }}
                  >
                    {reminder.note}
                  </Typography>
                )}
              </Box>
              <Stack direction="row" sx={{ flexShrink: 0 }}>
                {onDone && (
                  <Tooltip title={t('reminder.done.button')}>
                    <IconButton
                      size="small"
                      color="primary"
                      aria-label={t('reminder.done.button')}
                      onClick={() => onDone(reminder)}
                    >
                      <TaskAltRounded fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
                {onEdit && (
                  <Tooltip title={t('button.edit')}>
                    <IconButton
                      size="small"
                      aria-label={t('button.edit')}
                      onClick={() => onEdit(reminder)}
                    >
                      <EditOutlined fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
                {onDelete && (
                  <Tooltip title={t('button.delete')}>
                    <IconButton
                      size="small"
                      aria-label={t('button.delete')}
                      onClick={() => onDelete(reminder)}
                    >
                      <DeleteOutlineRounded fontSize="small" />
                    </IconButton>
                  </Tooltip>
                )}
              </Stack>
            </Stack>
          </Fragment>
        );
      })}
    </Stack>
  );
}
