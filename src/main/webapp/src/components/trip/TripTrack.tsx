import { Skeleton, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { Trip } from '@/generated';
import { getTripTrackOptions } from '@/generated/@tanstack/react-query.gen';
import { useFormat } from '@/utils/format';
import { localClient } from '@/utils/QueryClient';
import LazyTrackMap from '@/components/track/LazyTrackMap';
import SpeedLegend from '@/components/track/SpeedLegend';
import useFullscreen, { fullscreenSx } from '@/components/track/useFullscreen';

/**
 * The GPS track during a trip and its GPS distance next to the odometer
 * distance: only a hint, the trip log counts the odometers
 */
export default function TripTrack({
  carId,
  trip,
  styleUrl,
}: {
  carId: number;
  trip: Trip;
  styleUrl: string;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { fullscreen, toggle } = useFullscreen();
  const { data: track, isLoading } = useQuery(
    getTripTrackOptions({
      client: localClient,
      path: { carId, id: trip.id },
    }),
  );

  if (isLoading || !track) {
    return <Skeleton variant="rounded" height={320} sx={{ borderRadius: 3 }} />;
  }
  if (track.points === 0) {
    return (
      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
        {t('trip.track.empty')}
      </Typography>
    );
  }

  return (
    <Stack
      spacing={1.5}
      data-testid="trip-track"
      sx={fullscreen ? fullscreenSx : undefined}
    >
      {trip.deletedAt && (
        <Typography variant="body2" sx={{ color: 'error.main' }}>
          {t('trip.track.deleted')}
        </Typography>
      )}
      <Typography variant="body2">
        {t('trip.track.distance', {
          gps: format.distance(track.distance, 1),
        })}
        {trip.distance != null && (
          <>
            {' · '}
            {t('trip.track.odometer', {
              odometer: format.distance(trip.distance),
            })}
          </>
        )}
      </Typography>
      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        {t('trip.track.hint')}
      </Typography>
      <LazyTrackMap
        styleUrl={styleUrl}
        track={track}
        height={320}
        fill={fullscreen}
        fullscreen={fullscreen}
        onFullscreen={toggle}
      />
      <SpeedLegend />
    </Stack>
  );
}
