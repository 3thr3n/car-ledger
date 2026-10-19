import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Trip } from '@/generated';
import { deleteTripMutation } from '@/generated/@tanstack/react-query.gen';
import { isOfflineError } from '@/offline';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import { invalidateCar, localClient } from '@/utils/QueryClient';

/** the limit of the API */
const MAX_REASON = 500;

/**
 * Deletes a trip with a reason. It stays in the trip log, marked as deleted,
 * and no longer counts for the odometer. Only online: the owner decides, not a
 * queue that is sent later.
 */
export default function TripDeleteDialog({
  trip,
  onClose,
}: {
  /** the dialog is closed without */
  trip?: Trip;
  onClose: () => void;
}) {
  return (
    <Dialog open={!!trip} onClose={onClose} maxWidth="xs" fullWidth>
      {trip && <DeleteForm key={trip.id} trip={trip} onClose={onClose} />}
    </Dialog>
  );
}

function DeleteForm({ trip, onClose }: { trip: Trip; onClose: () => void }) {
  const { t } = useTranslation();
  const format = useFormat();
  const [reason, setReason] = useState('');
  const mutation = useMutation({
    ...deleteTripMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('trip.delete.deleted'));
      onClose();
      await invalidateCar(trip.carId);
    },
    onError: (error) =>
      toast.error(
        isOfflineError(error)
          ? t('trip.delete.offline')
          : errorMessage(error, t('errors.backend')),
      ),
  });

  return (
    <>
      <DialogTitle>{t('trip.delete.title')}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          {t('trip.delete.text', {
            time: format.dateTime(trip.startedAt),
            odometer: format.distance(trip.startOdometer),
          })}
        </DialogContentText>
        <TextField
          label={t('trip.delete.reason')}
          value={reason}
          onChange={(event) => setReason(event.target.value)}
          required
          multiline
          fullWidth
          autoFocus
          slotProps={{ htmlInput: { maxLength: MAX_REASON } }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          color="error"
          disabled={!reason.trim() || mutation.isPending}
          onClick={() =>
            mutation.mutate({
              path: { carId: trip.carId, id: trip.id },
              body: { reason: reason.trim() },
            })
          }
        >
          {t('trip.delete.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
