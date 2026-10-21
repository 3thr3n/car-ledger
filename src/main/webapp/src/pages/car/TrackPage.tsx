import { Alert, IconButton, Stack, Tooltip, Typography } from '@mui/material';
import ChevronLeftRounded from '@mui/icons-material/ChevronLeftRounded';
import ChevronRightRounded from '@mui/icons-material/ChevronRightRounded';
import MapRounded from '@mui/icons-material/MapRounded';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  getMapSettingsOptions,
  getTrackOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toApiDate, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import DateRangeFields from '@/components/dashboard/DateRangeFields';
import LazyTrackMap from '@/components/track/LazyTrackMap';
import SpeedLegend from '@/components/track/SpeedLegend';

/** the server returns at most this many days */
export const MAX_DAYS = 31;

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

  const { data: track } = useQuery({
    ...getTrackOptions({
      client: localClient,
      path: { carId },
      query: {
        from: from?.startOf('day').toISOString() ?? '',
        to: to?.add(1, 'day').startOf('day').toISOString() ?? '',
      },
    }),
    enabled: valid && !!settings?.styleUrl,
    // the old track stays on the map while the next one loads
    placeholderData: keepPreviousData,
  });

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
          />
        )}
        <SpeedLegend />
      </Stack>
    </Section>
  );
}
