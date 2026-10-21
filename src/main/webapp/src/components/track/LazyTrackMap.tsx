import { Skeleton } from '@mui/material';
import { lazy, Suspense } from 'react';
import type { TrackMapProps } from './TrackMap';

// MapLibre is loaded with the first map, not with the app
const TrackMap = lazy(() => import('./TrackMap'));

export default function LazyTrackMap(props: TrackMapProps) {
  return (
    <Suspense
      fallback={
        <Skeleton
          variant="rounded"
          height={props.height ?? 480}
          sx={{ borderRadius: 3 }}
        />
      }
    >
      <TrackMap {...props} />
    </Suspense>
  );
}
