import {
  Box,
  Button,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  deleteTracksMutation,
  setTrackRetentionMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import ConfirmDialog from '@/components/ui/ConfirmDialog';

/** days to choose from, forever is empty */
const PERIODS = [7, 30, 90, 180, 365, 730];
const FOREVER = 'forever';

/**
 * How long the track points of the car are kept, and deleting them all: they
 * are personal data
 */
export default function TrackHistory({
  carId,
  retentionDays,
}: {
  carId: number;
  retentionDays?: number | null;
}) {
  const { t } = useTranslation();
  const [shorter, setShorter] = useState<number>();
  const [deleting, setDeleting] = useState(false);
  const current = retentionDays ?? undefined;
  // a period set over the API stays selectable
  const periods =
    current && !PERIODS.includes(current)
      ? [...PERIODS, current].sort((a, b) => a - b)
      : PERIODS;

  const retention = useMutation({
    ...setTrackRetentionMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('track.history.saved'));
      await invalidateCar(carId);
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  const remove = useMutation({
    ...deleteTracksMutation({ client: localClient }),
    onSuccess: async ({ points }) => {
      toast.success(t('track.history.deleted', { count: points }));
      await invalidateCar(carId);
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  const save = (days: number | undefined) =>
    retention.mutate({ path: { carId }, body: { days } });

  const choose = (value: string) => {
    const days = value === FOREVER ? undefined : Number(value);
    // keeping them for less deletes points tonight
    if (days !== undefined && (current === undefined || days < current)) {
      setShorter(days);
    } else {
      save(days);
    }
  };

  const period = (days: number) =>
    days % 365 === 0
      ? t('track.history.years', { count: days / 365 })
      : t('track.history.days', { count: days });

  return (
    <Box data-testid="track-history">
      <Typography sx={{ fontWeight: 600 }}>
        {t('track.history.title')}
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
        {t('track.history.text')}
      </Typography>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{ alignItems: { sm: 'flex-start' } }}
      >
        <TextField
          select
          label={t('track.history.keep')}
          value={current === undefined ? FOREVER : String(current)}
          onChange={(e) => choose(e.target.value)}
          disabled={retention.isPending}
          helperText={
            current === undefined
              ? t('track.history.foreverHelp')
              : t('track.history.periodHelp')
          }
          sx={{ flexGrow: 1 }}
        >
          <MenuItem value={FOREVER}>{t('track.history.forever')}</MenuItem>
          {periods.map((days) => (
            <MenuItem key={days} value={String(days)}>
              {period(days)}
            </MenuItem>
          ))}
        </TextField>
        <Button
          color="error"
          variant="outlined"
          onClick={() => setDeleting(true)}
          disabled={remove.isPending}
          sx={{ flexShrink: 0, mt: { sm: 1 } }}
        >
          {t('track.history.delete')}
        </Button>
      </Stack>
      <ConfirmDialog
        open={shorter !== undefined}
        title={t('track.history.shorterTitle', {
          period: shorter && period(shorter),
        })}
        text={t('track.history.shorterText', {
          period: shorter && period(shorter),
        })}
        confirmLabel={t('track.history.shorterConfirm', {
          period: shorter && period(shorter),
        })}
        onClose={() => setShorter(undefined)}
        onConfirm={() => save(shorter)}
      />
      <ConfirmDialog
        open={deleting}
        title={t('track.history.deleteTitle')}
        text={t('track.history.deleteText')}
        confirmLabel={t('track.history.delete')}
        onClose={() => setDeleting(false)}
        onConfirm={() => remove.mutate({ path: { carId } })}
      />
    </Box>
  );
}
