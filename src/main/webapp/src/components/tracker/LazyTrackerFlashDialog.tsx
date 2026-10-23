import { lazy, Suspense } from 'react';
import type { TrackerFlashDialogProps } from './TrackerFlashDialog';

// esptool-js is loaded with the first flashing, not with the app
const TrackerFlashDialog = lazy(() => import('./TrackerFlashDialog'));

export default function LazyTrackerFlashDialog(props: TrackerFlashDialogProps) {
  if (!props.open) return null;
  return (
    <Suspense>
      <TrackerFlashDialog {...props} />
    </Suspense>
  );
}
