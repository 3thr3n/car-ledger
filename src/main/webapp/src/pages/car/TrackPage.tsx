import {
  Alert,
  Box,
  Button,
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
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
import PlaceDialog from '@/components/track/PlaceDialog';
import {
  Range,
  rangeOf,
  SHORTCUTS,
  shortcutOf,
} from '@/components/track/periods';
import useFullscreen, { fullscreenSx } from '@/components/track/useFullscreen';
import DriveList from '@/components/track/DriveList';
import { deleteRange, driveAt, interval } from '@/components/track/drives';
import TripPastDialog from '@/components/trip/TripPastDialog';
import { Place, TrackDrive, TripSuggestion } from '@/generated';

/** the server returns at most this many days */
export const MAX_DAYS = 31;

type Mode = 'speed' | 'frequency';

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

  const { data: drives } = useQuery({
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

  const { data: track, isPlaceholderData } = useQuery({
    ...getTrackOptions({
      client: localClient,
      path: { carId },
      query: selection,
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
    >
      <ToggleButton value="speed" sx={{ px: 1.5 }}>
        {t('track.mode.speed')}
      </ToggleButton>
      <ToggleButton value="frequency" sx={{ px: 1.5 }}>
        {t('track.mode.frequency')}
      </ToggleButton>
    </ToggleButtonGroup>
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

  return (
    <Section
      title={t('track.title')}
      subtitle={
        track && valid
          ? t('track.summary', {
              distance: format.distance(track.distance, 1),
              count: track.segments.length,
            })
          : t('track.subtitle')
      }
      // the fullscreen covers only the area below: there the switch is in it
      action={!fullscreen && modeSwitch}
    >
      <Stack
        spacing={2}
        data-testid="track-area"
        sx={fullscreen ? fullscreenSx : undefined}
      >
        <Stack
          direction="row"
          sx={{
            flexWrap: 'wrap',
            gap: 1,
            alignItems: 'center',
            justifyContent: 'space-between',
          }}
        >
          <ToggleButtonGroup
            size="small"
            exclusive
            value={shortcutOf(range) ?? null}
            onChange={(_, shortcut) => shortcut && setRange(rangeOf(shortcut))}
            aria-label={t('track.shortcuts.label')}
            sx={{ flexWrap: 'wrap' }}
          >
            {SHORTCUTS.map((shortcut) => (
              <ToggleButton key={shortcut} value={shortcut} sx={{ px: 1.5 }}>
                {t(`track.shortcuts.${shortcut}`)}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
          {fullscreen && modeSwitch}
        </Stack>
        <Stack
          direction="row"
          spacing={1}
          sx={{ alignItems: 'center', maxWidth: 640 }}
        >
          <Tooltip title={t('track.earlier')}>
            <span>
              <IconButton
                aria-label={t('track.earlier')}
                onClick={() => shift(-1)}
                disabled={!valid}
              >
                <ChevronLeftRounded />
              </IconButton>
            </span>
          </Tooltip>
          <DateRangeFields
            from={range.from}
            to={range.to}
            onChange={setRange}
            stacked
          />
          <Tooltip title={t('track.later')}>
            <span>
              <IconButton
                aria-label={t('track.later')}
                onClick={() => shift(1)}
                disabled={!valid || !to || !to.isBefore(dayjs(), 'day')}
              >
                <ChevronRightRounded />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
        {days > MAX_DAYS && (
          <Alert severity="info">
            {t('track.tooLong', { days: MAX_DAYS })}
          </Alert>
        )}
        {settings?.styleUrl && track && valid && track.points === 0 && (
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('track.empty')}
          </Typography>
        )}
        {settings?.styleUrl && (
          <LazyTrackMap
            styleUrl={settings.styleUrl}
            track={track ?? { segments: [], distance: 0, points: 0 }}
            frequency={
              mode === 'frequency'
                ? (frequency ?? { lines: [], maxPasses: 0 })
                : undefined
            }
            places={showPlaces ? places : undefined}
            onPlaceClick={role('WRITE') ? setToName : undefined}
            highlight={hovered}
            onTrackClick={pick}
            fill={fullscreen}
            fullscreen={fullscreen}
            onFullscreen={toggle}
          />
        )}
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={1}
          sx={{
            alignItems: { sm: 'center' },
            justifyContent: 'space-between',
          }}
        >
          <Stack
            direction={{ xs: 'column', md: 'row' }}
            spacing={{ xs: 1, md: 3 }}
            sx={{ flexGrow: 1, alignItems: { md: 'center' } }}
          >
            {mode === 'speed' ? (
              <SpeedLegend />
            ) : (
              <FrequencyLegend maxPasses={frequency?.maxPasses ?? 0} />
            )}
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
                sx={{ mr: 0, whiteSpace: 'nowrap' }}
              />
            </Tooltip>
          </Stack>
          {/* not while the track of another period is loading, and not in
              the fullscreen, which is for looking at the map */}
          {track &&
            valid &&
            track.points > 0 &&
            !isPlaceholderData &&
            !fullscreen &&
            role('MANAGE') && (
              <Button
                color="error"
                size="small"
                startIcon={<DeleteOutlineRounded />}
                onClick={() => setDeleting(true)}
                disabled={remove.isPending}
                sx={{ alignSelf: { xs: 'flex-start', sm: 'center' } }}
              >
                {t('track.deletePeriod')}
              </Button>
            )}
        </Stack>
      </Stack>
      {drives && valid && (
        <Box sx={{ mt: 3 }}>
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
        </Box>
      )}
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
    </Section>
  );
}
