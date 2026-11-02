import {
  Box,
  Divider,
  IconButton,
  Stack,
  Theme,
  Tooltip,
  Typography,
} from '@mui/material';
import EventRounded from '@mui/icons-material/EventRounded';
import SpeedRounded from '@mui/icons-material/SpeedRounded';
import RepeatRounded from '@mui/icons-material/RepeatRounded';
import TaskAltRounded from '@mui/icons-material/TaskAltRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import ReportProblemRounded from '@mui/icons-material/ReportProblemRounded';
import NotificationsNoneRounded from '@mui/icons-material/NotificationsNoneRounded';
import { Fragment, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Reminder, ReminderStatus } from '@/generated';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import { RouterLink } from '@/components/ui/links';
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

/** the light of the status: lit red when overdue, amber when due soon */
const toneSx = (status: ReminderStatus) => (theme: Theme) =>
  status === ReminderStatus.OVERDUE
    ? { color: theme.vars.palette.error.main }
    : status === ReminderStatus.DUE_SOON
      ? {
          color: theme.vars.palette.warning.main,
          ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
        }
      : { color: theme.vars.palette.text.secondary };

/**
 * The reminders as the service display of a car: a warning light by the
 * status, what is due and when, and how far it is as the figure
 */
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
        const date = text.dateParts(reminder);
        const odometer = text.odometerParts(reminder);
        const repeat = !compact && text.repeatText(reminder);
        const lit = reminder.status !== ReminderStatus.UPCOMING;

        return (
          <Fragment key={reminder.id}>
            {index > 0 && <Divider />}
            <Box
              data-testid="reminder"
              sx={{
                display: 'grid',
                gridTemplateColumns: {
                  xs: 'auto minmax(0, 1fr) auto',
                  sm: 'auto minmax(0, 1fr) auto auto',
                },
                gridTemplateAreas: {
                  xs: '"light text actions" ". count count"',
                  sm: '"light text count actions"',
                },
                columnGap: 1.5,
                rowGap: 0.5,
                alignItems: 'start',
                py: 1.5,
              }}
            >
              <Box
                aria-hidden
                sx={[
                  toneSx(reminder.status),
                  { gridArea: 'light', display: 'flex', pt: 0.25 },
                ]}
              >
                {lit ? <ReportProblemRounded /> : <NotificationsNoneRounded />}
              </Box>
              <Box sx={{ gridArea: 'text', minWidth: 0 }}>
                <Typography sx={{ fontWeight: 600 }}>
                  {reminder.title}{' '}
                  <Typography
                    component="span"
                    variant="body2"
                    sx={[toneSx(reminder.status), { fontWeight: 600 }]}
                  >
                    {t(`reminder.status.${reminder.status}`)}
                  </Typography>
                </Typography>
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
                  {date && (
                    <Detail icon={<EventRounded />}>{date.absolute}</Detail>
                  )}
                  {odometer && (
                    <Detail icon={<SpeedRounded />}>{odometer.absolute}</Detail>
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
              {/* how far it is, as the figure of the service display */}
              <Box
                className="tabular"
                sx={[
                  lit ? toneSx(reminder.status) : { color: 'text.primary' },
                  {
                    gridArea: 'count',
                    textAlign: { sm: 'right' },
                    fontFamily: displayFont,
                    fontWeight: 650,
                    fontSize: '1.125rem',
                    lineHeight: 1.3,
                    whiteSpace: 'nowrap',
                  },
                ]}
              >
                {date && <Box>{date.relative}</Box>}
                {odometer && <Box>{odometer.relative}</Box>}
              </Box>
              <Stack direction="row" sx={{ gridArea: 'actions' }}>
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
            </Box>
          </Fragment>
        );
      })}
    </Stack>
  );
}
