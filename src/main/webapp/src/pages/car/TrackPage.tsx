import {
  Alert,
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
  getPlacesOptions,
  getTrackFrequencyOptions,
  getTrackOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
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
import { Place } from '@/generated';

/** the server returns at most this many days */
export const MAX_DAYS = 31;

type Mode = 'speed' | 'frequency';

/**
 * The GPS tracks of the car on a day or in a period, in the local time zone
 * of the device
 */
export default function TrackPage({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const today = toApiDate(dayjs())!;
  const [range, setRange] = useState<{ from?: string; to?: string }>({
    from: today,
    to: today,
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
  const [mode, setMode] = useState<Mode>('speed');
  const [showPlaces, setShowPlaces] = useState(false);
  const [toName, setToName] = useState<Place>();

  const { data: track, isPlaceholderData } = useQuery({
    ...getTrackOptions({
      client: localClient,
      path: { carId },
      query: period,
    }),
    enabled: valid && !!settings?.styleUrl,
    // the old track stays on the map while the next one loads
    placeholderData: keepPreviousData,
  });

  const { data: frequency } = useQuery({
    ...getTrackFrequencyOptions({
      client: localClient,
      path: { carId },
      query: period,
    }),
    enabled: mode === 'frequency' && valid && !!settings?.styleUrl,
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
      action={
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
      }
    >
      <Stack spacing={2}>
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
            onPlaceClick={setToName}
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
          {/* not while the track of another period is loading */}
          {track && valid && track.points > 0 && !isPlaceholderData && (
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
    </Section>
  );
}
