import {
  Box,
  Button,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AddRoadRounded from '@mui/icons-material/AddRoadRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import { useTranslation } from 'react-i18next';
import type { TrackDrive } from '@/generated';
import { useFormat } from '@/utils/format';
import { displayFont } from '@/theme';
import { byDay, Leg, legsOf, minutes } from './drives';
import useDuration from './useDuration';

export interface DriveListProps {
  /** newest first */
  drives: TrackDrive[];
  /** the starts of the selected drives, none for all */
  selected: string[];
  onToggle: (drive: TrackDrive) => void;
  onShowAll: () => void;
  /** the drive under the pointer or the focus, highlighted on the map */
  onHover: (drive?: TrackDrive) => void;
  /** only for who may delete the track */
  onDelete?: (drive: TrackDrive) => void;
  /**
   * only for who may add trips to a car with the trip log, for the drives
   * without a trip
   */
  onAddTrip?: (drive: TrackDrive) => void;
}

/** the column of the times, the one of the line, the text */
const COLUMNS = '4.75rem 1.5rem minmax(0, 1fr)';

/** a place the route stops at: a ring on the line, its name and how long */
function Stop({ name, parked }: { name?: string; parked?: string }) {
  const { t } = useTranslation();
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: COLUMNS,
        alignItems: 'center',
        minHeight: 32,
      }}
    >
      <Box
        aria-hidden
        sx={(theme) => ({
          gridColumn: 2,
          justifySelf: 'center',
          width: 14,
          height: 14,
          borderRadius: '50%',
          border: `3px solid ${theme.vars.palette.text.primary}`,
          bgcolor: 'background.paper',
        })}
      />
      <Stack
        direction="row"
        spacing={1}
        sx={{
          gridColumn: 3,
          pl: 1,
          alignItems: 'baseline',
          justifyContent: 'space-between',
          minWidth: 0,
        }}
      >
        <Typography
          noWrap
          sx={{
            fontWeight: 600,
            ...(!name && { fontWeight: 400, color: 'text.secondary' }),
          }}
        >
          {name ?? t('track.drives.unnamed')}
        </Typography>
        {parked && (
          <Typography
            variant="body2"
            noWrap
            sx={{ color: 'text.secondary', flexShrink: 0 }}
          >
            {t('track.drives.parked', { duration: parked })}
          </Typography>
        )}
      </Stack>
    </Box>
  );
}

/**
 * The drives of the period by day, each day as its route from stop to stop:
 * picking drives shows only them on the map, none shows all
 */
export default function DriveList({
  drives,
  selected,
  onToggle,
  onShowAll,
  onHover,
  onDelete,
  onAddTrip,
}: DriveListProps) {
  const { t } = useTranslation();
  const format = useFormat();

  const title = (drive: TrackDrive) =>
    `${format.time(drive.start)} – ${format.time(drive.end)}`;
  const duration = useDuration();
  const dayName = (day: string) =>
    new Intl.DateTimeFormat(format.locale, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
    }).format(new Date(`${day}T12:00:00`));

  const leg = ({ drive, departure, parked }: Leg) => {
    const checked = selected.includes(drive.start);
    const label = title(drive);
    const addTrip = onAddTrip && !drive.trip;
    return (
      <Box
        component="li"
        key={drive.start}
        data-testid="track-drive"
        onMouseEnter={() => onHover(drive)}
      >
        {departure && <Stop name={drive.from} />}
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: `${COLUMNS} auto`,
            alignItems: 'stretch',
          }}
        >
          {/* the leg of the route picks the drive, like a checkbox */}
          <Box
            component="button"
            type="button"
            role="checkbox"
            aria-checked={checked}
            aria-label={label}
            onClick={() => onToggle(drive)}
            onFocus={() => onHover(drive)}
            onBlur={() => onHover(undefined)}
            sx={(theme) => ({
              gridColumn: '1 / 4',
              display: 'grid',
              gridTemplateColumns: COLUMNS,
              alignItems: 'center',
              minHeight: 56,
              p: 0,
              border: 0,
              borderRadius: 1,
              bgcolor: checked
                ? `rgba(${theme.vars.palette.primary.mainChannel} / 0.1)`
                : 'transparent',
              color: 'inherit',
              font: 'inherit',
              textAlign: 'left',
              cursor: 'pointer',
              '& .line': {
                bgcolor: checked ? 'primary.main' : 'text.disabled',
              },
              '&:hover': {
                bgcolor: `rgba(${theme.vars.palette.primary.mainChannel} / ${checked ? 0.14 : 0.05})`,
              },
              '&:hover .line': { bgcolor: 'primary.main' },
              '&:focus-visible': {
                outline: `2px solid ${theme.vars.palette.primary.main}`,
                outlineOffset: -2,
              },
            })}
          >
            <Box
              className="tabular"
              sx={{
                pl: 1,
                fontFamily: displayFont,
                lineHeight: 1.2,
                whiteSpace: 'nowrap',
                display: 'flex',
                flexDirection: 'column',
                justifyContent: 'space-between',
                alignSelf: 'stretch',
                py: 0.5,
              }}
            >
              <Box component="span" sx={{ fontWeight: 650 }}>
                {format.time(drive.start)}
              </Box>
              <Box component="span" sx={{ color: 'text.secondary' }}>
                {format.time(drive.end)}
              </Box>
            </Box>
            <Box
              aria-hidden
              className="line"
              sx={{
                justifySelf: 'center',
                alignSelf: 'stretch',
                width: 6,
                borderRadius: 3,
                transition: 'background-color 120ms',
              }}
            />
            <Typography
              component="span"
              variant="body2"
              sx={{ pl: 1, color: 'text.secondary' }}
            >
              {duration(minutes(drive))}, {format.distance(drive.distance, 1)}
            </Typography>
          </Box>
          {(addTrip || onDelete) && (
            <Stack direction="row" sx={{ alignItems: 'center' }}>
              {addTrip && (
                <Tooltip title={t('track.drives.addTrip')}>
                  <IconButton
                    aria-label={t('track.drives.addTripLabel', {
                      drive: label,
                    })}
                    onClick={() => onAddTrip(drive)}
                    sx={{ color: 'text.secondary' }}
                  >
                    <AddRoadRounded />
                  </IconButton>
                </Tooltip>
              )}
              {onDelete && (
                <Tooltip title={t('track.drives.delete')}>
                  <IconButton
                    aria-label={t('track.drives.deleteLabel', {
                      drive: label,
                    })}
                    onClick={() => onDelete(drive)}
                    sx={{ color: 'text.secondary' }}
                  >
                    <DeleteOutlineRounded />
                  </IconButton>
                </Tooltip>
              )}
            </Stack>
          )}
        </Box>
        <Stop name={drive.to} parked={parked ? duration(parked) : undefined} />
      </Box>
    );
  };

  return (
    <Box data-testid="track-drives">
      <Stack
        direction="row"
        spacing={1}
        sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}
      >
        <Typography variant="h6" component="h3">
          {selected.length
            ? t('track.drives.selected', {
                count: selected.length,
                total: drives.length,
              })
            : t('track.drives.title', { count: drives.length })}
        </Typography>
        {selected.length > 0 && (
          <Button size="small" onClick={onShowAll}>
            {t('track.drives.showAll')}
          </Button>
        )}
      </Stack>
      {drives.length === 0 ? (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('track.drives.empty')}
        </Typography>
      ) : (
        <Stack spacing={3} onMouseLeave={() => onHover(undefined)}>
          {byDay(drives).map(({ day, drives: ofDay }) => (
            <Box component="section" key={day} aria-label={dayName(day)}>
              <Stack
                direction="row"
                spacing={1}
                sx={{
                  alignItems: 'baseline',
                  justifyContent: 'space-between',
                  pb: 0.75,
                  mb: 0.5,
                  borderBottom: 1,
                  borderColor: 'divider',
                }}
              >
                <Typography
                  component="h4"
                  sx={{ fontFamily: displayFont, fontWeight: 650 }}
                >
                  {dayName(day)}
                </Typography>
                <Typography
                  className="tabular"
                  variant="body2"
                  sx={{ color: 'text.secondary', whiteSpace: 'nowrap' }}
                >
                  {format.distance(
                    ofDay.reduce((sum, drive) => sum + drive.distance, 0),
                    1,
                  )}
                </Typography>
              </Stack>
              <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>
                {legsOf(ofDay).map(leg)}
              </Box>
            </Box>
          ))}
        </Stack>
      )}
    </Box>
  );
}
