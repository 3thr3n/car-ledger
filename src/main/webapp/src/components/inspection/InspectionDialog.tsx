import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  InspectionResult,
  TechnicalInspection,
  TechnicalInspectionInput,
} from '@/generated';
import {
  addTechnicalInspectionMutation,
  updateTechnicalInspectionMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate, useFormat } from '@/utils/format';
import { odometerToApi, odometerToField } from '@/utils/units';
import NumberField from '@/components/bill/NumberField';
import TestingBodyField from '@/components/ui/TestingBodyField';

/** months to the next HU, after the first one of a new car it is 36 */
const INTERVAL = 24;

export interface InspectionDialogProps {
  open: boolean;
  carId: number;
  /** current odometer of the car */
  odometer?: number;
  /** edits this one, adds a new one without */
  inspection?: TechnicalInspection;
  onClose: () => void;
}

export default function InspectionDialog({
  open,
  onClose,
  ...props
}: InspectionDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      {/* keyed, so the form starts fresh for every inspection */}
      {open && (
        <InspectionForm
          key={props.inspection?.id ?? 'new'}
          onClose={onClose}
          {...props}
        />
      )}
    </Dialog>
  );
}

const nextOf = (date: Dayjs | null) =>
  date?.isValid() ? date.add(INTERVAL, 'month').startOf('month') : null;

function InspectionForm({
  carId,
  odometer,
  inspection,
  onClose,
}: Omit<InspectionDialogProps, 'open'>) {
  const { t } = useTranslation();
  const { units } = useFormat();
  const [date, setDate] = useState<Dayjs | null>(
    inspection ? dayjs(inspection.date) : dayjs(),
  );
  const [odometerValue, setOdometerValue] = useState(
    odometerToField(units, inspection ? inspection.odometer : odometer),
  );
  const [testingBody, setTestingBody] = useState(inspection?.testingBody ?? '');
  const [result, setResult] = useState(
    inspection?.result ?? InspectionResult.NO_DEFECTS,
  );
  const [defects, setDefects] = useState(inspection?.defects ?? '');
  const [nextDue, setNextDue] = useState<Dayjs | null>(
    inspection
      ? inspection.nextDue
        ? dayjs(inspection.nextDue)
        : null
      : nextOf(dayjs()),
  );
  // the next due month follows the date until it is chosen
  const [nextDueChosen, setNextDueChosen] = useState(!!inspection);

  const onSuccess = async () => {
    toast.success(t(inspection ? 'inspection.updated' : 'inspection.added'));
    onClose();
    await invalidateCar(carId);
  };
  const onError = (error: Error) =>
    toast.error(errorMessage(error, t('errors.backend')));
  const add = useMutation({
    ...addTechnicalInspectionMutation({ client: localClient }),
    onSuccess,
    onError,
  });
  const update = useMutation({
    ...updateTechnicalInspectionMutation({ client: localClient }),
    onSuccess,
    onError,
  });

  const changeDate = (value: Dayjs | null) => {
    setDate(value);
    if (!nextDueChosen) setNextDue(nextOf(value));
  };

  const nextDueError =
    nextDue?.isValid() &&
    date?.isValid() &&
    !nextDue.startOf('month').isAfter(date.startOf('month'))
      ? t('inspection.errors.nextDue')
      : undefined;
  const invalid = !date?.isValid() || !!nextDueError;
  const withDefects = result !== InspectionResult.NO_DEFECTS;

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    if (invalid) return;
    const body: TechnicalInspectionInput = {
      date: toApiDate(date),
      odometer: odometerToApi(units, odometerValue),
      testingBody: testingBody.trim() || undefined,
      result,
      defects: (withDefects && defects.trim()) || undefined,
      nextDue: nextDue?.isValid()
        ? toApiDate(nextDue.startOf('month'))
        : undefined,
    };
    if (inspection) {
      update.mutate({ path: { carId, id: inspection.id }, body });
    } else {
      add.mutate({ path: { carId }, body });
    }
  };

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <DialogTitle>
        {t(inspection ? 'inspection.edit.title' : 'inspection.add.title')}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <DatePicker
              label={t('inspection.fields.date')}
              value={date}
              onChange={changeDate}
              disableFuture
              slotProps={{ textField: { fullWidth: true, required: true } }}
            />
            <NumberField
              label={t('inspection.fields.odometer')}
              value={odometerValue}
              onChange={setOdometerValue}
              decimalScale={0}
              suffix={` ${units.distance.label}`}
              max={10_000_000}
            />
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
            <TestingBodyField
              label={t('inspection.fields.testingBody')}
              value={testingBody}
              onChange={setTestingBody}
            />
            <TextField
              select
              fullWidth
              label={t('inspection.fields.result')}
              value={result}
              onChange={(e) => setResult(e.target.value as InspectionResult)}
            >
              {Object.values(InspectionResult).map((each) => (
                <MenuItem key={each} value={each}>
                  {t(`inspection.results.${each}`)}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
          {withDefects && (
            <TextField
              label={t('inspection.fields.defects')}
              value={defects}
              onChange={(e) => setDefects(e.target.value)}
              multiline
              minRows={2}
              helperText={t(`inspection.resultHints.${result}`)}
              slotProps={{ htmlInput: { maxLength: 2000 } }}
            />
          )}
          <DatePicker
            label={t('inspection.fields.nextDue')}
            value={nextDue}
            onChange={(value) => {
              setNextDue(value);
              setNextDueChosen(true);
            }}
            views={['year', 'month']}
            openTo="month"
            slotProps={{
              textField: {
                fullWidth: true,
                error: !!nextDueError,
                helperText: nextDueError ?? t('inspection.nextDueHint'),
              },
              field: { clearable: true },
            }}
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
          disabled={invalid || add.isPending || update.isPending}
        >
          {t('button.save')}
        </Button>
      </DialogActions>
    </Box>
  );
}
