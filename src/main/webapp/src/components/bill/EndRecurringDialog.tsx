import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { RecurringBill } from '@/generated';
import { endRecurringBillMutation } from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate } from '@/utils/format';

export interface EndRecurringDialogProps {
  carId: number;
  bill?: RecurringBill;
  onClose: () => void;
}

/**
 * Ends a recurring bill, the end date is the first day without payments
 */
export default function EndRecurringDialog({
  carId,
  bill,
  onClose,
}: EndRecurringDialogProps) {
  const { t } = useTranslation();
  const [endDate, setEndDate] = useState<Dayjs | null>(dayjs());

  const mutation = useMutation({
    ...endRecurringBillMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('bill.end.done'));
      onClose();
      await invalidateCar(carId);
    },
    onError: (error) => {
      toast.error(errorMessage(error, t('errors.backend')));
    },
  });

  const startDate = bill?.startDate ? dayjs(bill.startDate) : undefined;
  const invalid =
    !endDate?.isValid() || (startDate && endDate.isBefore(startDate, 'day'));

  return (
    <Dialog open={!!bill} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('bill.end.title', { name: bill?.name })}</DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2.5 }}>
          {t('bill.end.text')}
        </DialogContentText>
        <DatePicker
          label={t('bill.fields.endDate')}
          value={endDate}
          onChange={setEndDate}
          minDate={startDate}
          slotProps={{ textField: { fullWidth: true } }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={invalid || mutation.isPending}
          onClick={() =>
            bill?.id !== undefined &&
            mutation.mutate({
              path: { carId, billId: bill.id },
              body: { endDate: toApiDate(endDate) },
            })
          }
        >
          {t('bill.end.confirm')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
