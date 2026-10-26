import { Alert, AlertTitle, Button, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useFormat } from '@/utils/format';
import { offlineQueue } from '@/offline';
import { QueuedRequest } from '@/offline/queue';
import { useQueued } from '@/offline/useQueued';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import Section from '@/components/ui/Section';

/**
 * The entries made offline for a car the user no longer has, e.g. a driver
 * whose access ended before they were sent: the server refuses them, they stay
 * on the device to look at until the user discards them
 */
export default function OrphanedEntries() {
  const { t } = useTranslation();
  const entries = useQueued();
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));
  const [toDiscard, setToDiscard] = useState<QueuedRequest[]>();

  // only with the list of the server, not while it loads
  const orphaned = cars
    ? entries.filter((entry) => !cars.some((car) => car.id === entry.carId))
    : [];
  if (orphaned.length === 0) return null;

  return (
    <Section
      title={t('offline.orphaned.title')}
      subtitle={t('offline.orphaned.subtitle')}
      action={
        <Button
          color="inherit"
          size="small"
          onClick={() => setToDiscard(orphaned)}
        >
          {t('offline.orphaned.discardAll')}
        </Button>
      }
      sx={{ mb: 3 }}
    >
      <Stack spacing={1} sx={{ pb: 2 }} data-testid="orphaned-entries">
        {orphaned.map((entry) => (
          <Alert
            key={entry.id}
            severity="warning"
            variant="outlined"
            action={
              <Button
                color="inherit"
                size="small"
                onClick={() => setToDiscard([entry])}
              >
                {t('offline.list.discard')}
              </Button>
            }
          >
            <AlertTitle>
              <EntryText entry={entry} />
            </AlertTitle>
            {t('offline.orphaned.refused')}
          </Alert>
        ))}
      </Stack>
      <ConfirmDialog
        open={!!toDiscard}
        title={t('offline.others.discardTitle', {
          count: toDiscard?.length ?? 0,
        })}
        text={t('offline.list.discardText')}
        confirmLabel={t('offline.list.discard')}
        onClose={() => setToDiscard(undefined)}
        onConfirm={() =>
          toDiscard &&
          void Promise.all(
            toDiscard.map((entry) => offlineQueue.discard(entry.id)),
          )
        }
      />
    </Section>
  );
}

/** what the entry was: the day and the amount of a bill, a trip's odometer */
function EntryText({ entry }: { entry: QueuedRequest }) {
  const { t } = useTranslation();
  const format = useFormat();
  const { body } = entry;

  if (entry.tripAction) {
    const start = entry.tripAction === 'start';
    return (
      <>
        {format.dateTime(
          (start ? body.startedAt : body.endedAt) as string | undefined,
        )}{' '}
        ·{' '}
        {t(start ? 'trip.pending.start' : 'trip.pending.finish', {
          odometer: format.distance(
            (start ? body.startOdometer : body.endOdometer) as
              number | undefined,
          ),
        })}
      </>
    );
  }
  const total = body.total as number | undefined;
  return (
    <>
      {format.date(body.date as string)}
      {entry.billType && ` · ${t(`bill.types.${entry.billType}`)}`}
      {total !== undefined && ` · ${format.money(total)}`}
    </>
  );
}
