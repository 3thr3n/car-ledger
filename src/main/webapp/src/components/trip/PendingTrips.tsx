import { Alert, AlertTitle, Button, Stack } from '@mui/material';
import CloudOffRounded from '@mui/icons-material/CloudOffRounded';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { offlineQueue } from '@/offline';
import { QueuedRequest } from '@/offline/queue';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import Section from '@/components/ui/Section';
import { useFormat } from '@/utils/format';
import { finishEntryId } from './tripState';
import { useQueuedTrips } from './useTrip';

/**
 * The starts and finishes of the car kept on the device and not sent yet. One
 * the server rejected can only be discarded: a trip is not edited.
 */
export default function PendingTrips({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const entries = useQueuedTrips(carId);
  const [toDiscard, setToDiscard] = useState<QueuedRequest>();

  // without its start, the finish of a trip would be rejected
  const discard = async (entry: QueuedRequest) => {
    await offlineQueue.discard(entry.id);
    if (entry.tripAction === 'start' && entry.tripId) {
      await offlineQueue.discard(finishEntryId(entry.tripId));
    }
  };

  if (entries.length === 0) return null;

  return (
    <Section
      title={t('trip.pending.title')}
      subtitle={t('trip.pending.subtitle')}
    >
      <Stack spacing={1} sx={{ pb: 2 }} data-testid="pending-trips">
        {entries.map((entry) => {
          const failed = entry.status === 'failed';
          const start = entry.tripAction === 'start';
          const time = (start ? entry.body.startedAt : entry.body.endedAt) as
            string | undefined;
          const odometer = (
            start ? entry.body.startOdometer : entry.body.endOdometer
          ) as number | undefined;
          return (
            <Alert
              key={entry.id}
              severity={failed ? 'error' : 'info'}
              variant="outlined"
              icon={failed ? undefined : <CloudOffRounded />}
              action={
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => setToDiscard(entry)}
                >
                  {t('offline.list.discard')}
                </Button>
              }
            >
              <AlertTitle>
                {format.dateTime(time)} ·{' '}
                {t(start ? 'trip.pending.start' : 'trip.pending.finish', {
                  odometer: format.distance(odometer),
                })}
              </AlertTitle>
              {failed
                ? entry.error || t('offline.list.rejected')
                : t('offline.list.waiting')}
            </Alert>
          );
        })}
      </Stack>
      <ConfirmDialog
        open={!!toDiscard}
        title={t('offline.list.discardTitle')}
        text={t('offline.list.discardText')}
        confirmLabel={t('offline.list.discard')}
        onClose={() => setToDiscard(undefined)}
        onConfirm={() => toDiscard && void discard(toDiscard)}
      />
    </Section>
  );
}
