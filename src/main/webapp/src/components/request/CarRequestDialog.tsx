import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { requestMoreCarsMutation } from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import NumberField from '@/components/bill/NumberField';

/** the same limit as the backend */
const MAX_LIMIT = 100;
const MAX_TEXT = 500;

/**
 * Asks the administrator for more cars than the limit allows
 */
export default function CarRequestDialog({
  open,
  maxCars,
  onClose,
}: {
  open: boolean;
  /** the current limit */
  maxCars: number;
  onClose: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {/* a fresh form every time it opens */}
      {open && <CarRequestForm maxCars={maxCars} onClose={onClose} />}
    </Dialog>
  );
}

function CarRequestForm({
  maxCars,
  onClose,
}: {
  maxCars: number;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [cars, setCars] = useState<number | undefined>(1);
  const [reason, setReason] = useState('');
  const maxMore = MAX_LIMIT - maxCars;

  const request = useMutation({
    ...requestMoreCarsMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('carRequest.sent'));
      onClose();
      return queryClient.invalidateQueries({
        predicate: (query) =>
          (query.queryKey[0] as { _id?: string } | undefined)?._id ===
          'getMyLatestCarRequest',
      });
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
  });

  const invalid = !cars || cars < 1 || cars > maxMore;

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    if (invalid) return;
    request.mutate({ body: { cars, reason: reason.trim() || undefined } });
  };

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <DialogTitle>{t('carRequest.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5}>
          <DialogContentText>
            {t('carRequest.text', { count: maxCars })}
          </DialogContentText>
          <NumberField
            label={t('carRequest.cars')}
            value={cars}
            onChange={setCars}
            decimalScale={0}
            max={maxMore}
            required
          />
          <TextField
            label={t('carRequest.reason')}
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            helperText={t('carRequest.reasonHelp')}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: MAX_TEXT } }}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          type="submit"
          variant="contained"
          disabled={invalid || request.isPending}
        >
          {t('carRequest.send')}
        </Button>
      </DialogActions>
    </Box>
  );
}
