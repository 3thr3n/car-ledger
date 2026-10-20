import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  IconButton,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import RestoreRounded from '@mui/icons-material/RestoreRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { OdometerReset } from '@/generated';
import {
  deleteOdometerResetMutation,
  getMyCarOptions,
  getOdometerResetsOptions,
} from '@/generated/@tanstack/react-query.gen';
import Section from '@/components/ui/Section';
import { isOfflineError } from '@/offline';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import OdometerResetDialog from './OdometerResetDialog';
import { MAX_PURPOSE } from './tripForm';

/** the readings of a deleted reset don't count */
const struck = (reset: OdometerReset) =>
  reset.deletedAt
    ? { textDecoration: 'line-through', color: 'text.disabled' }
    : {};

/**
 * The odometer resets of the car, the latest first: a replaced instrument
 * cluster or a reset odometer. Hidden while there is none and the trip log is
 * off.
 */
export default function OdometerResets({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const [adding, setAdding] = useState(false);
  const [toDelete, setToDelete] = useState<OdometerReset>();
  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  const { data: resets = [] } = useQuery(
    getOdometerResetsOptions({ client: localClient, path: { carId } }),
  );

  if (!car?.tripLog && resets.length === 0) return null;

  return (
    <Section
      flush
      title={t('trip.reset.listTitle')}
      subtitle={t('trip.reset.listSubtitle')}
      action={
        car?.tripLog && (
          <Button size="small" onClick={() => setAdding(true)}>
            {t('trip.reset.button')}
          </Button>
        )
      }
    >
      {resets.length === 0 ? (
        <Typography
          variant="body2"
          sx={{ color: 'text.secondary', px: 2.5, pb: 2 }}
        >
          {t('trip.reset.empty')}
        </Typography>
      ) : (
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small" data-testid="odometer-resets">
            <TableHead>
              <TableRow>
                <TableCell>{t('trip.list.date')}</TableCell>
                <TableCell>{t('trip.list.route')}</TableCell>
                <TableCell>{t('trip.reset.reason')}</TableCell>
                <TableCell padding="checkbox" />
              </TableRow>
            </TableHead>
            <TableBody>
              {resets.map((reset) => (
                <TableRow key={reset.id}>
                  <TableCell sx={{ whiteSpace: 'nowrap' }}>
                    {format.dateTime(reset.resetAt)}
                    {reset.restored && (
                      <Tooltip title={t('trip.list.restored')}>
                        <RestoreRounded
                          fontSize="small"
                          color="action"
                          aria-label={t('trip.list.restored')}
                          sx={{ ml: 0.5, verticalAlign: 'middle' }}
                        />
                      </Tooltip>
                    )}
                  </TableCell>
                  <TableCell
                    className="tabular"
                    sx={{ ...struck(reset), whiteSpace: 'nowrap' }}
                  >
                    {reset.odometerBefore == null
                      ? '–'
                      : format.distance(reset.odometerBefore)}{' '}
                    → {format.distance(reset.odometer)}
                  </TableCell>
                  <TableCell>
                    {reset.deletedAt && (
                      <Typography variant="body2" sx={{ color: 'error.main' }}>
                        {t('trip.list.deleted', {
                          date: format.dateTime(reset.deletedAt),
                          reason: reset.deleteReason,
                        })}
                      </Typography>
                    )}
                    {reset.reason}
                  </TableCell>
                  <TableCell padding="checkbox">
                    {!reset.deletedAt && (
                      <IconButton
                        size="small"
                        aria-label={t('trip.reset.delete')}
                        title={t('trip.reset.delete')}
                        onClick={() => setToDelete(reset)}
                      >
                        <DeleteOutlineRounded fontSize="small" />
                      </IconButton>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Box>
      )}
      <OdometerResetDialog
        carId={carId}
        open={adding}
        onClose={() => setAdding(false)}
      />
      <Dialog
        open={!!toDelete}
        onClose={() => setToDelete(undefined)}
        maxWidth="xs"
        fullWidth
      >
        {toDelete && (
          <DeleteForm
            key={toDelete.id}
            reset={toDelete}
            onClose={() => setToDelete(undefined)}
          />
        )}
      </Dialog>
    </Section>
  );
}

/**
 * Deletes a reset with a reason, like a trip: it stays listed and no longer
 * counts. Only online.
 */
function DeleteForm({
  reset,
  onClose,
}: {
  reset: OdometerReset;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const [reason, setReason] = useState('');
  const mutation = useMutation({
    ...deleteOdometerResetMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('trip.reset.deleted'));
      onClose();
      await invalidateCar(reset.carId);
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
      <DialogTitle>{t('trip.reset.deleteTitle')}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          {t('trip.reset.deleteText', {
            time: format.dateTime(reset.resetAt),
            odometer: format.distance(reset.odometer),
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
          slotProps={{ htmlInput: { maxLength: MAX_PURPOSE } }}
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
              path: { carId: reset.carId, id: reset.id },
              body: { reason: reason.trim() },
            })
          }
        >
          {t('trip.reset.delete')}
        </Button>
      </DialogActions>
    </>
  );
}
