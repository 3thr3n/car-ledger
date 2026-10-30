import {
  Box,
  Button,
  Checkbox,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AddRoadRounded from '@mui/icons-material/AddRoadRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import { useTranslation } from 'react-i18next';
import type { TrackDrive } from '@/generated';
import { useFormat } from '@/utils/format';
import { byDay, minutes } from './drives';

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

/**
 * The drives of the period by day: picking some shows only them on the map,
 * none shows all
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
  const details = (drive: TrackDrive) =>
    [
      drive.from || drive.to
        ? `${drive.from ?? t('track.drives.unnamed')} → ${drive.to ?? t('track.drives.unnamed')}`
        : undefined,
      t('track.drives.minutes', { count: minutes(drive) }),
      format.distance(drive.distance, 1),
    ]
      .filter(Boolean)
      .join(' · ');

  return (
    <Box data-testid="track-drives">
      <Stack
        direction="row"
        spacing={1}
        sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 0.5 }}
      >
        <Typography sx={{ fontWeight: 600 }}>
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
        <List
          dense
          disablePadding
          onMouseLeave={() => onHover(undefined)}
          sx={{ maxHeight: 420, overflow: 'auto' }}
        >
          {byDay(drives).map(({ day, drives: ofDay }) => (
            <li key={day}>
              <ul style={{ padding: 0 }}>
                <ListSubheader sx={{ lineHeight: '32px', px: 1 }}>
                  {format.date(day)}
                </ListSubheader>
                {ofDay.map((drive) => {
                  const checked = selected.includes(drive.start);
                  const label = title(drive);
                  const addTrip = onAddTrip && !drive.trip;
                  const actions = (addTrip ? 1 : 0) + (onDelete ? 1 : 0);
                  return (
                    <ListItem
                      key={drive.start}
                      disablePadding
                      data-testid="track-drive"
                      secondaryAction={
                        actions > 0 && (
                          <Stack direction="row">
                            {addTrip && (
                              <Tooltip title={t('track.drives.addTrip')}>
                                <IconButton
                                  edge={onDelete ? undefined : 'end'}
                                  aria-label={t('track.drives.addTripLabel', {
                                    drive: label,
                                  })}
                                  onClick={() => onAddTrip(drive)}
                                >
                                  <AddRoadRounded />
                                </IconButton>
                              </Tooltip>
                            )}
                            {onDelete && (
                              <Tooltip title={t('track.drives.delete')}>
                                <IconButton
                                  edge="end"
                                  aria-label={t('track.drives.deleteLabel', {
                                    drive: label,
                                  })}
                                  onClick={() => onDelete(drive)}
                                >
                                  <DeleteOutlineRounded />
                                </IconButton>
                              </Tooltip>
                            )}
                          </Stack>
                        )
                      }
                      onMouseEnter={() => onHover(drive)}
                    >
                      <ListItemButton
                        role={undefined}
                        selected={checked}
                        onClick={() => onToggle(drive)}
                        onFocus={() => onHover(drive)}
                        onBlur={() => onHover(undefined)}
                        // room for the buttons on the right, 48px for one
                        sx={{
                          borderRadius: 1,
                          ...(actions > 0 && { pr: actions * 5 + 1 }),
                        }}
                      >
                        <ListItemIcon sx={{ minWidth: 36 }}>
                          <Checkbox
                            edge="start"
                            size="small"
                            checked={checked}
                            tabIndex={-1}
                            disableRipple
                            slotProps={{
                              input: { 'aria-label': label },
                            }}
                          />
                        </ListItemIcon>
                        <ListItemText
                          primary={label}
                          secondary={details(drive)}
                        />
                      </ListItemButton>
                    </ListItem>
                  );
                })}
              </ul>
            </li>
          ))}
        </List>
      )}
    </Box>
  );
}
