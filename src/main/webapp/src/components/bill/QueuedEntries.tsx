import { Alert, AlertTitle, Button, Stack } from '@mui/material';
import CloudOffRounded from '@mui/icons-material/CloudOffRounded';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { offlineQueue } from '@/offline';
import { QueuedRequest } from '@/offline/queue';
import { useQueued } from '@/offline/useQueued';
import { useFormat } from '@/utils/format';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import Section from '@/components/ui/Section';
import { RouterButton } from '@/components/ui/links';

/**
 * The new bills of the car that are kept on the device and not sent yet. A
 * bill the server rejected is corrected in the form or discarded.
 */
export default function QueuedEntries({
  carId,
  type,
}: {
  carId: number;
  type: BillType;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const entries = useQueued(carId, type);
  const [toDiscard, setToDiscard] = useState<QueuedRequest>();

  if (entries.length === 0) return null;

  return (
    <Section
      title={t('offline.list.title')}
      subtitle={t('offline.list.subtitle')}
      sx={{ mb: 2 }}
    >
      <Stack spacing={1} sx={{ pb: 2 }} data-testid="queued-entries">
        {entries.map((entry) => {
          const failed = entry.status === 'failed';
          const total = entry.body.total as number | undefined;
          return (
            <Alert
              key={entry.id}
              severity={failed ? 'error' : 'info'}
              variant="outlined"
              icon={failed ? undefined : <CloudOffRounded />}
              action={
                <Stack direction="row" spacing={1}>
                  {failed && (
                    <RouterButton
                      to="/car/$id/bill/add"
                      params={{ id: `${carId}` }}
                      search={{ type, queued: entry.id }}
                      color="inherit"
                      size="small"
                    >
                      {t('offline.list.correct')}
                    </RouterButton>
                  )}
                  <Button
                    color="inherit"
                    size="small"
                    onClick={() => setToDiscard(entry)}
                  >
                    {t('offline.list.discard')}
                  </Button>
                </Stack>
              }
            >
              <AlertTitle>
                {format.date(entry.body.date as string)}
                {total !== undefined && ` · ${format.money(total)}`}
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
        onConfirm={() => toDiscard && void offlineQueue.discard(toDiscard.id)}
      />
    </Section>
  );
}
