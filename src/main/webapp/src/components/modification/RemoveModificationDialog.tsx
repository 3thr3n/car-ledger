import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Stack,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Modification } from '@/generated';
import { updateModificationMutation } from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate, useFormat } from '@/utils/format';
import { odometerToApi, odometerToField } from '@/utils/units';
import NumberField from '@/components/bill/NumberField';
import { toInput } from './approval';

export interface RemoveModificationDialogProps {
  modification?: Modification;
  /** current odometer of the car */
  odometer?: number;
  onClose: () => void;
}

/**
 * Takes a part off the car: it moves into the history with the date and the
 * odometer, unlike deleting it
 */
export default function RemoveModificationDialog({
  modification,
  odometer,
  onClose,
}: RemoveModificationDialogProps) {
  return (
    <Dialog open={!!modification} onClose={onClose} maxWidth="xs" fullWidth>
      {modification && (
        <RemoveForm
          key={modification.id}
          modification={modification}
          odometer={odometer}
          onClose={onClose}
        />
      )}
    </Dialog>
  );
}

function RemoveForm({
  modification,
  odometer,
  onClose,
}: RemoveModificationDialogProps & { modification: Modification }) {
  const { t } = useTranslation();
  const { units } = useFormat();
  const [date, setDate] = useState<Dayjs | null>(dayjs());
  const [removedOdometer, setRemovedOdometer] = useState(
    odometerToField(units, odometer),
  );
  const installed = modification.installedDate
    ? dayjs(modification.installedDate)
    : undefined;
  const installedOdometer = odometerToField(
    units,
    modification.installedOdometer,
  );
  const dateError =
    date?.isValid() && installed && date.isBefore(installed, 'day')
      ? t('modification.errors.removedBeforeInstalled')
      : undefined;
  const odometerError =
    removedOdometer !== undefined &&
    installedOdometer !== undefined &&
    removedOdometer < installedOdometer
      ? t('modification.errors.removedBelowInstalled')
      : undefined;

  const mutation = useMutation({
    ...updateModificationMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('modification.remove.success'));
      onClose();
      await invalidateCar(modification.carId);
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
  });

  const remove = () => {
    // the input replaces all fields, the rest stays as it is
    mutation.mutate({
      path: { carId: modification.carId, id: modification.id },
      body: {
        ...toInput(modification),
        removedDate: toApiDate(date),
        removedOdometer: odometerToApi(units, removedOdometer),
      },
    });
  };

  return (
    <>
      <DialogTitle>
        {t('modification.remove.title', { part: modification.part })}
      </DialogTitle>
      <DialogContent>
        <DialogContentText sx={{ mb: 2.5 }}>
          {t('modification.remove.text')}
        </DialogContentText>
        <Stack spacing={2.5}>
          <DatePicker
            label={t('modification.remove.date')}
            value={date}
            onChange={setDate}
            disableFuture
            slotProps={{
              textField: {
                fullWidth: true,
                error: !!dateError,
                helperText: dateError,
              },
            }}
          />
          <NumberField
            label={t('modification.fields.odometer')}
            value={removedOdometer}
            onChange={setRemovedOdometer}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={10_000_000}
            error={odometerError}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={
            !date?.isValid() ||
            !!dateError ||
            !!odometerError ||
            mutation.isPending
          }
          onClick={remove}
        >
          {t('modification.remove.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
