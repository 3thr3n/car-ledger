import {
  Alert,
  Box,
  Button,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  Theme,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
  useMediaQuery,
} from '@mui/material';
import ChevronLeftRounded from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import MapRounded from '@mui/icons-material/MapRounded';
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  deleteTracksMutation,
  getMapSettingsOptions,
  getMyCarOptions,
  getPlacesOptions,
  getTrackDrivesOptions,
  getTrackFrequencyOptions,
  getTrackOptions,
  getTripSuggestionForDriveOptions,
} from '@/generated/@tanstack/react-query.gen';
import useCarRole from '@/hooks/useCarRole';
import queryClient, { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import { toApiDate, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import DateRangeFields from '@/components/dashboard/DateRangeFields';
import LazyTrackMap from '@/components/track/LazyTrackMap';
import SpeedLegend from '@/components/track/SpeedLegend';
import FrequencyLegend from '@/components/track/FrequencyLegend';
import GForceLegend from '@/components/track/GForceLegend';
import PlaceDialog from '@/components/track/PlaceDialog';
import {
  Range,
  rangeOf,
  SHORTCUTS,
  shortcutOf,
} from '@/components/track/periods';
import useFullscreen, { fullscreenSx } from '@/components/track/useFullscreen';
import DriveList from '@/components/track/DriveList';
import useDuration from '@/components/track/useDuration';
import {
  deleteRange,
  driveAt,
  interval,
  minutes,
} from '@/components/track/drives';
import TripPastDialog from '@/components/trip/TripPastDialog';
import { Place, TrackDrive, TripSuggestion } from '@/generated';
import { visuallyHidden } from '@/components/ui/visuallyHidden';
import { CLUSTER_AMBER, displayFont } from '@/theme';

/** the server returns at most this many days */
export const MAX_DAYS = 31;

type Mode = 'speed' | 'frequency' | 'gforce';

/** a panel over the map: the switch of the mode, the legend */
const overlaySx = (theme: Theme) => ({
  bgcolor: `rgba(${theme.vars.palette.background.paperChannel} / 0.94)`,
  backdropFilter: 'blur(6px)',
  borderRadius: '10px',
  boxShadow: `0 0 0 1px ${theme.vars.palette.divider}, 0 2px 8px rgba(0 0 0 / 0.12)`,
});

/** in the URL, so a reload or a shared link shows the same */
export interface TrackSearch {
  /** yyyy-MM-dd, both or the last days */
  from?: string;
  to?: string;
  /** the starts of the selected drives, all without */
  drives?: string[];
}

/**
 * The GPS tracks of the car on a day or in a period, in the local time zone
 * of the device, all of them or the drives picked from the list
 */
export default function TrackPage({
  carId,
  search,
  onSearchChange,
}: {
  carId: number;
  search: TrackSearch;
  onSearchChange: (search: TrackSearch) => void;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const duration = useDuration();
  const wide = useMediaQuery((theme: Theme) => theme.breakpoints.up('sm'));
  // the last days by default: a drive yesterday evening still shows
  const [range, setRangeState] = useState<Range>(() =>
    search.from && search.to
      ? { from: search.from, to: search.to }
      : rangeOf('last3'),
  );
  const selected = search.drives ?? [];
  /** another period, the selection of the old one is gone */
  const setRange = (next: Range) => {
    setRangeState(next);
    // while one of the fields is being typed, the URL keeps the last period
    if (next.from && next.to) onSearchChange({ from: next.from, to: next.to });
  };
  const select = (drives: string[]) =>
    onSearchChange({
      from: range.from,
      to: range.to,
      drives: drives.length ? drives : undefined,
    });

  const { data: settings } = useQuery(
    getMapSettingsOptions({ client: localClient }),
  );

  const from = range.from ? dayjs(range.from) : undefined;
  const to = range.to ? dayjs(range.to) : undefined;
  const days = from && to ? to.diff(from, 'day') + 1 : 0;
  const valid = days >= 1 && days <= MAX_DAYS;

  const period = {
    from: from?.startOf('day').toISOString() ?? '',
    to: to?.add(1, 'day').startOf('day').toISOString() ?? '',
  };
  const [deleting, setDeleting] = useState(false);
  const { fullscreen, toggle } = useFullscreen();
  // a viewer sees the tracks and the places, the owner deletes them
  const role = useCarRole(carId);
  const [mode, setMode] = useState<Mode>('speed');
  const [showPlaces, setShowPlaces] = useState(false);
  const [toName, setToName] = useState<Place>();
  const [hovered, setHovered] = useState<TrackDrive>();
  const [toDelete, setToDelete] = useState<TrackDrive>();
  const [addingTrip, setAddingTrip] = useState<TripSuggestion>();
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );

  const { data: drives, error: drivesError } = useQuery({
    ...getTrackDrivesOptions({
      client: localClient,
      path: { carId },
      query: period,
    }),
    enabled: valid && !!settings?.styleUrl,
  });
  // drives of a link that aren't there anymore, e.g. deleted, don't count
  const selectedDrives = (drives ?? []).filter((drive) =>
    selected.includes(drive.start),
  );
  const selection = {
    ...period,
    drive: selectedDrives.length ? selectedDrives.map(interval) : undefined,
  };
  // the selection needs the ends of its drives
  const ready = valid && !!settings?.styleUrl && (!selected.length || !!drives);

  const {
    data: track,
    isPlaceholderData,
    error: trackError,
  } = useQuery({
    ...getTrackOptions({
      client: localClient,
      path: { carId },
      // only for the mode: they make a busy month larger
      query: mode === 'gforce' ? { ...selection, gforces: true } : selection,
    }),
    enabled: ready,
    // the old track stays on the map while the next one loads
    placeholderData: keepPreviousData,
  });

  const { data: frequency } = useQuery({
    ...getTrackFrequencyOptions({
      client: localClient,
      path: { carId },
      query: selection,
    }),
    enabled: mode === 'frequency' && ready,
    placeholderData: keepPreviousData,
  });

  const { data: places } = useQuery({
    ...getPlacesOptions({
      client: localClient,
      path: { carId },
      query: period,
    }),
    enabled: showPlaces && valid && !!settings?.styleUrl,
    placeholderData: keepPreviousData,
  });

  const remove = useMutation({
    ...deleteTracksMutation({ client: localClient }),
    onSuccess: async ({ points }) => {
      toast.success(t('track.history.deleted', { count: points }));
      await invalidateCar(carId);
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  const removeDrive = useMutation({
    ...deleteTracksMutation({ client: localClient }),
    onSuccess: async ({ points }, { query }) => {
      toast.success(t('track.drives.deleted', { count: points }));
      select(selected.filter((start) => start !== query?.from));
      await invalidateCar(carId);
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  const toggleDrive = (drive: TrackDrive) =>
    select(
      selected.includes(drive.start)
        ? selected.filter((start) => start !== drive.start)
        : [...selected, drive.start],
    );

  /** prefilled like a trip suggestion, with the odometers known before it */
  const addTrip = async (drive: TrackDrive) => {
    try {
      setAddingTrip(
        await queryClient.query(
          getTripSuggestionForDriveOptions({
            client: localClient,
            path: { carId },
            query: { start: drive.start, end: drive.end },
          }),
        ),
      );
    } catch (e) {
      toast.error(errorMessage(e, t('errors.backend')));
    }
  };

  /** a click on the track picks its drive */
  const pick = (ts: string) => {
    const drive = driveAt(drives ?? [], ts);
    if (drive && !selected.includes(drive.start)) toggleDrive(drive);
  };

  const shown =
    days === 1
      ? format.date(range.from)
      : `${format.date(range.from)} – ${format.date(range.to)}`;

  /** the same amount of days before or after */
  const shift = (direction: -1 | 1) =>
    setRange({
      from: toApiDate(from!.add(direction * days, 'day')),
      to: toApiDate(to!.add(direction * days, 'day')),
    });

  const modeSwitch = (
    <ToggleButtonGroup
      size="small"
      exclusive
      value={mode}
      onChange={(_, value: Mode | null) => value && setMode(value)}
      aria-label={t('track.mode.label')}
      sx={overlaySx}
    >
      <ToggleButton value="speed" sx={{ px: 1.5 }}>
        {t('track.mode.speed')}
      </ToggleButton>
      <ToggleButton value="frequency" sx={{ px: 1.5 }}>
        {t('track.mode.frequency')}
      </ToggleButton>
      <ToggleButton value="gforce" sx={{ px: 1.5 }}>
        {t('track.mode.gforce')}
      </ToggleButton>
    </ToggleButtonGroup>
  );

  // on the map from a tablet on, below it on a phone, where it would cover it
  const legend = (
    <Stack spacing={1} role="group" aria-label={t('track.legend')}>
      {mode === 'speed' && <SpeedLegend stacked />}
      {mode === 'frequency' && (
        <FrequencyLegend maxPasses={frequency?.maxPasses ?? 0} />
      )}
      {mode === 'gforce' && <GForceLegend stacked />}
      <Tooltip title={t('track.places.showHint')}>
        <FormControlLabel
          control={
            <Switch
              size="small"
              checked={showPlaces}
              onChange={(_, checked) => setShowPlaces(checked)}
            />
          }
          label={t('track.places.show')}
          sx={{ mr: 0, ml: -0.5, whiteSpace: 'nowrap', alignSelf: 'start' }}
        />
      </Tooltip>
    </Stack>
  );

  if (settings && !settings.styleUrl) {
    return (
      <Section>
        <EmptyState
          icon={<MapRounded />}
          title={t('track.notSetUp')}
          text={t('track.notSetUpText')}
        />
      </Section>
    );
  }

  // the drives of the distance, of the selection or all
  const driven = selectedDrives.length ? selectedDrives : (drives ?? []);
  const atTheWheel = driven.reduce((sum, drive) => sum + minutes(drive), 0);
  const empty = !!track && valid && track.points === 0;

  return (
    <>
      <Box
        data-testid="track-area"
        sx={[
          {
            display: 'grid',
            gridTemplateColumns: {
              xs: 'minmax(0, 1fr)',
              md: 'minmax(0, 1fr) 24rem',
            },
            gap: { xs: 2.5, md: 3 },
            alignItems: 'start',
          },
          ...(fullscreen
            ? [
                fullscreenSx,
                { display: 'grid', gridTemplateRows: { md: 'minmax(0, 1fr)' } },
              ]
            : []),
        ]}
      >
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            gap: 1.5,
            // the map stays while the drives next to it scroll
            ...(fullscreen
              ? { height: { xs: '70vh', md: '100%' } }
              : {
                  position: { md: 'sticky' },
                  top: { md: 24 },
                  height: { xs: '60vh', md: 'calc(100dvh - 48px)' },
                  minHeight: 360,
                }),
          }}
        >
          <Box
            sx={(theme) => ({
              position: 'relative',
              flex: 1,
              minHeight: 0,
              display: 'flex',
              flexDirection: 'column',
              overflow: 'hidden',
              borderRadius: '10px',
              border: 1,
              borderColor: 'divider',
              '& [data-testid="track-map"]': { borderRadius: 0 },
              // the controls of MapLibre lit like the panels of the cluster
              ...theme.applyStyles('dark', {
                borderRadius: '20px',
                '& .maplibregl-ctrl-group': {
                  bgcolor: 'background.paper',
                  boxShadow: `0 0 0 1px ${theme.vars.palette.divider}`,
                },
                '& .maplibregl-ctrl-group button + button': {
                  borderTopColor: theme.vars.palette.divider,
                },
                '& .maplibregl-ctrl-icon, & .maplibregl-ctrl-attrib-button': {
                  filter: 'invert(1)',
                },
                '& .maplibregl-ctrl-attrib.maplibregl-compact, & .maplibregl-ctrl-attrib':
                  {
                    bgcolor: `rgba(${theme.vars.palette.background.paperChannel} / 0.85)`,
                    color: 'text.secondary',
                    '& a': { color: 'inherit' },
                  },
              }),
            })}
          >
            {settings?.styleUrl && (
              <LazyTrackMap
                styleUrl={settings.styleUrl}
                track={track ?? { segments: [], distance: 0, points: 0 }}
                frequency={
                  mode === 'frequency'
                    ? (frequency ?? { lines: [], maxPasses: 0 })
                    : undefined
                }
                // the track of the speeds stays until the one with them is loaded
                gForces={mode === 'gforce' && !isPlaceholderData}
                places={showPlaces ? places : undefined}
                onPlaceClick={role('WRITE') ? setToName : undefined}
                highlight={hovered}
                onTrackClick={pick}
                fill
                fullscreen={fullscreen}
                onFullscreen={toggle}
              />
            )}
            <Box sx={{ position: 'absolute', top: 10, left: 10 }}>
              {modeSwitch}
            </Box>
            {wide && (
              <Box
                sx={[
                  overlaySx,
                  {
                    position: 'absolute',
                    left: 10,
                    bottom: 10,
                    width: 300,
                    px: 1.75,
                    pt: 1.25,
                    pb: 0.75,
                  },
                ]}
              >
                {legend}
              </Box>
            )}
          </Box>
          {!wide && legend}
        </Box>

        <Stack
          component="section"
          aria-labelledby="track-title"
          spacing={3}
          sx={
            fullscreen ? { minHeight: 0, overflowY: { md: 'auto' } } : undefined
          }
        >
          <Typography id="track-title" variant="h6" sx={visuallyHidden}>
            {t('track.title')}
          </Typography>
          <Stack spacing={1.5}>
            <ToggleButtonGroup
              size="small"
              exclusive
              fullWidth
              value={shortcutOf(range) ?? null}
              onChange={(_, shortcut) =>
                shortcut && setRange(rangeOf(shortcut))
              }
              aria-label={t('track.shortcuts.label')}
            >
              {SHORTCUTS.map((shortcut) => (
                <ToggleButton
                  key={shortcut}
                  value={shortcut}
                  sx={{ px: 1, lineHeight: 1.2 }}
                >
                  {t(`track.shortcuts.${shortcut}`)}
                </ToggleButton>
              ))}
            </ToggleButtonGroup>
            <Stack direction="row" spacing={0.5} sx={{ alignItems: 'center' }}>
              <Tooltip title={t('track.earlier')}>
                <span>
                  <IconButton
                    aria-label={t('track.earlier')}
                    onClick={() => shift(-1)}
                    disabled={!valid}
                    sx={{ ml: -1 }}
                  >
                    <ChevronLeftRounded />
                  </IconButton>
                </span>
              </Tooltip>
              {/* the fields shrink to the column next to the map */}
              <Box
                sx={{
                  flex: 1,
                  minWidth: 0,
                  display: 'flex',
                  '& .MuiStack-root, & .MuiFormControl-root, & .MuiPickersInputBase-root, & .MuiPickersSectionList-root':
                    { minWidth: 0 },
                }}
              >
                <DateRangeFields
                  from={range.from}
                  to={range.to}
                  onChange={setRange}
                  stacked
                />
              </Box>
              <Tooltip title={t('track.later')}>
                <span>
                  <IconButton
                    aria-label={t('track.later')}
                    onClick={() => shift(1)}
                    disabled={!valid || !to || !to.isBefore(dayjs(), 'day')}
                    sx={{ mr: -1 }}
                  >
                    <ChevronRightRounded />
                  </IconButton>
                </span>
              </Tooltip>
            </Stack>
          </Stack>

          {days > MAX_DAYS && (
            <Alert severity="info">
              {t('track.tooLong', { days: MAX_DAYS })}
            </Alert>
          )}
          {/* inline: the period stays changeable */}
          {valid && !!(drivesError ?? trackError) && (
            <Alert severity="error">
              {errorMessage(drivesError ?? trackError, t('errors.backend'))}
            </Alert>
          )}

          {empty && (
            <Typography sx={{ color: 'text.secondary' }}>
              {t('track.empty')}
            </Typography>
          )}
          {track && valid && !empty && (
            <Box data-testid="track-distance">
              <Typography
                className="tabular"
                sx={(theme) => ({
                  fontFamily: displayFont,
                  fontWeight: 700,
                  fontSize: '2.75rem',
                  lineHeight: 1.05,
                  ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
                })}
              >
                {format.distance(track.distance, 1)}
              </Typography>
              <Typography sx={{ color: 'text.secondary' }}>
                {atTheWheel
                  ? t('track.atTheWheel', { duration: duration(atTheWheel) })
                  : t('track.gpsDistance')}
              </Typography>
            </Box>
          )}

          {drives && valid && (
            <DriveList
              drives={drives}
              selected={selectedDrives.map((drive) => drive.start)}
              onToggle={toggleDrive}
              onShowAll={() => select([])}
              onHover={setHovered}
              onDelete={role('MANAGE') ? setToDelete : undefined}
              onAddTrip={
                car?.tripLog && role('WRITE')
                  ? (drive) => void addTrip(drive)
                  : undefined
              }
            />
          )}

          {/* not while the track of another period is loading, and not in
              the fullscreen, which is for looking at the map */}
          {track &&
            valid &&
            track.points > 0 &&
            !isPlaceholderData &&
            !fullscreen &&
            role('MANAGE') && (
              <Box sx={{ pt: 1, borderTop: 1, borderColor: 'divider' }}>
                <Button
                  color="error"
                  size="small"
                  startIcon={<DeleteOutlineRounded />}
                  onClick={() => setDeleting(true)}
                  disabled={remove.isPending}
                  sx={{ ml: -1 }}
                >
                  {t('track.deletePeriod')}
                </Button>
              </Box>
            )}
        </Stack>
      </Box>
      <TripPastDialog
        carId={carId}
        open={addingTrip !== undefined}
        initial={addingTrip}
        onClose={() => setAddingTrip(undefined)}
      />
      <PlaceDialog
        carId={carId}
        place={toName}
        onClose={() => setToName(undefined)}
      />
      <ConfirmDialog
        open={deleting}
        title={t('track.deletePeriodTitle', { period: shown })}
        text={t('track.deletePeriodText', { count: track?.points ?? 0 })}
        confirmLabel={t('track.deletePeriod')}
        onClose={() => setDeleting(false)}
        onConfirm={() => remove.mutate({ path: { carId }, query: period })}
      />
      <ConfirmDialog
        open={!!toDelete}
        title={t('track.drives.deleteTitle', {
          drive: toDelete
            ? `${format.date(toDelete.start)}, ${format.time(toDelete.start)} – ${format.time(toDelete.end)}`
            : '',
        })}
        text={t('track.drives.deleteText', {
          distance: format.distance(toDelete?.distance, 1),
        })}
        confirmLabel={t('track.drives.delete')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete &&
          removeDrive.mutate({
            path: { carId },
            query: deleteRange(drives ?? [], toDelete, period.to),
          })
        }
      />
    </>
  );
}
