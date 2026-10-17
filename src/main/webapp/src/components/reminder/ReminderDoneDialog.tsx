import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material';
import RepeatRounded from '@mui/icons-material/RepeatRounded';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { odometerToApi, odometerToField } from '@/utils/units';
import { toast } from 'react-toastify';
import { Reminder } from '@/generated';
import { completeReminderMutation } from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate, useFormat } from '@/utils/format';
import NumberField from '@/components/bill/NumberField';
import useReminderText from './useReminderText';

export interface ReminderDoneDialogProps {
  reminder?: Reminder;
  onClose: () => void;
}

/**
 * Marks a reminder as done. A repeating one is due again one interval later,
 * any other is removed.
 */
export default function ReminderDoneDialog({
  reminder,
  onClose,
}: ReminderDoneDialogProps) {
  return (
    <Dialog open={!!reminder} onClose={onClose} maxWidth="xs" fullWidth>
      {reminder && (
        <DoneForm key={reminder.id} reminder={reminder} onClose={onClose} />
      )}
    </Dialog>
  );
}

function DoneForm({
  reminder,
  onClose,
}: {
  reminder: Reminder;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const text = useReminderText();
  const [date, setDate] = useState<Dayjs | null>(dayjs());
  const { units } = useFormat();
  const [odometer, setOdometer] = useState<number | undefined>(
    odometerToField(units, reminder.odometer),
  );
  const repeat = text.repeatText(reminder);

  const mutation = useMutation({
    ...completeReminderMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('reminder.done.success'));
      onClose();
      await invalidateCar(reminder.carId);
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
  });

  return (
    <>
      <DialogTitle>
        {t('reminder.done.title', { title: reminder.title })}
      </DialogTitle>
      <DialogContent>
        {repeat && (
          <Stack
            direction="row"
            spacing={1}
            sx={{
              alignItems: 'center',
              mb: 1,
              '& svg': { fontSize: 18 },
            }}
          >
            <RepeatRounded color="primary" />
            <Typography
              sx={{
                fontWeight: 600,
              }}
            >
              {repeat}
            </Typography>
          </Stack>
        )}
        <DialogContentText sx={{ mb: 2.5 }}>
          {repeat ? t('reminder.done.repeating') : t('reminder.done.oneOff')}
        </DialogContentText>
        {repeat && (
          <Stack spacing={2.5}>
            <DatePicker
              label={t('reminder.done.date')}
              value={date}
              onChange={setDate}
              disableFuture
              slotProps={{ textField: { fullWidth: true } }}
            />
            {reminder.intervalKm != null && (
              <NumberField
                label={t('reminder.done.odometer')}
                value={odometer}
                onChange={setOdometer}
                decimalScale={0}
                suffix={` ${units.distance.label}`}
                max={10_000_000}
              />
            )}
          </Stack>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={(!!repeat && !date?.isValid()) || mutation.isPending}
          onClick={() =>
            mutation.mutate({
              path: { carId: reminder.carId, id: reminder.id },
              body: {
                date: repeat ? toApiDate(date) : undefined,
                odometer: repeat ? odometerToApi(units, odometer) : undefined,
              },
            })
          }
        >
          {t('reminder.done.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
