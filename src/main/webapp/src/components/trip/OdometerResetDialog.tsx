import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { addOdometerResetMutation } from '@/generated/@tanstack/react-query.gen';
import NumberField from '@/components/bill/NumberField';
import { isOfflineError } from '@/offline';
import { useFormat } from '@/utils/format';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { odometerToApi } from '@/utils/units';
import {
  MAX_ODOMETER,
  MAX_PURPOSE,
  OdometerConflict,
  useConflict,
} from './tripForm';

/**
 * Adds an odometer reset: a replaced instrument cluster or a reset odometer.
 * From its time on the odometer counts from the new reading. Only online, like
 * a past trip: the server checks it against the trips at once.
 */
export default function OdometerResetDialog({
  carId,
  open,
  onClose,
}: {
  carId: number;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {open && <ResetForm carId={carId} onClose={onClose} />}
    </Dialog>
  );
}

function ResetForm({ carId, onClose }: { carId: number; onClose: () => void }) {
  const { t } = useTranslation();
  const { units } = useFormat();
  // the same id when it is sent again, e.g. after a lost answer
  const [id] = useState(() => crypto.randomUUID());
  const [resetAt, setResetAt] = useState<Dayjs | null>(() => dayjs());
  const [odometer, setOdometer] = useState<number>();
  const [before, setBefore] = useState<number>();
  const [reason, setReason] = useState('');
  const [touched, setTouched] = useState(false);
  const [conflict, setConflict] = useState<OdometerConflict>({});
  const onConflict = useConflict();
  const mutation = useMutation({
    ...addOdometerResetMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('trip.reset.added'));
      onClose();
      await invalidateCar(carId);
    },
    onError: (error) => {
      if (isOfflineError(error)) toast.error(t('trip.reset.offline'));
      else setConflict(onConflict(error));
    },
  });

  const required = t('trip.errors.required');
  const errors = {
    resetAt: !resetAt?.isValid()
      ? required
      : resetAt.isAfter(dayjs())
        ? t('trip.errors.future')
        : undefined,
    odometer: odometer === undefined ? required : conflict.above,
    before: conflict.below,
    reason: reason.trim() ? undefined : required,
  };
  const invalid = Object.values(errors).some(Boolean);
  const shown = (error?: string) =>
    touched || error !== required ? error : undefined;

  const submit = () => {
    setTouched(true);
    if (invalid) return;
    mutation.mutate({
      path: { carId, id },
      body: {
        resetAt: resetAt!.toISOString(),
        odometer: odometerToApi(units, odometer),
        odometerBefore: odometerToApi(units, before),
        reason: reason.trim(),
      },
    });
  };

  return (
    <>
      <DialogTitle>{t('trip.reset.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <Alert severity="info" variant="outlined">
            {t('trip.reset.text')}
          </Alert>
          <DateTimePicker
            label={t('trip.reset.resetAt')}
            value={resetAt}
            onChange={setResetAt}
            disableFuture
            slotProps={{
              textField: {
                fullWidth: true,
                required: true,
                error: !!shown(errors.resetAt),
                helperText: shown(errors.resetAt),
              },
            }}
          />
          <NumberField
            label={t('trip.reset.before')}
            value={before}
            onChange={(next) => {
              setBefore(next);
              setConflict({});
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            error={errors.before}
            helperText={t('trip.reset.beforeHelp')}
          />
          <NumberField
            label={t('trip.reset.odometer')}
            value={odometer}
            onChange={(next) => {
              setOdometer(next);
              setConflict({});
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            required
            error={shown(errors.odometer)}
          />
          <TextField
            label={t('trip.reset.reason')}
            value={reason}
            onChange={(event) => setReason(event.target.value)}
            required
            multiline
            fullWidth
            error={!!shown(errors.reason)}
            helperText={shown(errors.reason) ?? t('trip.reset.reasonHelp')}
            slotProps={{ htmlInput: { maxLength: MAX_PURPOSE } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={mutation.isPending}
          onClick={submit}
        >
          {t('trip.reset.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
