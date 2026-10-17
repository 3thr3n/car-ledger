import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { useMutation } from '@tanstack/react-query';
import dayjs, { Dayjs } from 'dayjs';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Reminder, ReminderInput } from '@/generated';
import {
  addReminderMutation,
  updateReminderMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { toApiDate, useFormat } from '@/utils/format';
import { odometerToApi, odometerToField, Units } from '@/utils/units';
import NumberField from '@/components/bill/NumberField';

/** common reminders, they fill title and repetition */
const presets = [
  { key: 'inspection', intervalMonths: 24 },
  { key: 'service', intervalMonths: 12, intervalKm: 15000 },
  { key: 'tires', intervalMonths: 6 },
  { key: 'vignette', intervalMonths: 12 },
] as const;

export interface ReminderDialogProps {
  open: boolean;
  carId: number;
  /** current odometer of the car */
  odometer?: number;
  /** edits this one, adds a new one without */
  reminder?: Reminder;
  onClose: () => void;
}

export default function ReminderDialog({
  open,
  onClose,
  ...props
}: ReminderDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      {/* keyed, so the form starts fresh for every reminder */}
      {open && (
        <ReminderForm
          key={props.reminder?.id ?? 'new'}
          onClose={onClose}
          {...props}
        />
      )}
    </Dialog>
  );
}

/** the interval of a preset in round miles, e.g. 15,000 km are 9,500 mi */
function presetInterval(units: Units, km: number) {
  const value = units.distance.fromKm(km);
  return units.distance.label === 'km' ? value : Math.round(value / 500) * 500;
}

function ReminderForm({
  carId,
  odometer,
  reminder,
  onClose,
}: Omit<ReminderDialogProps, 'open'>) {
  const { t } = useTranslation();
  const format = useFormat();
  const { units } = format;
  const [title, setTitle] = useState(reminder?.title ?? '');
  const [dueDate, setDueDate] = useState<Dayjs | null>(
    reminder?.dueDate ? dayjs(reminder.dueDate) : null,
  );
  // missing values are null in the JSON, the form uses undefined; the
  // distances are in the units of the account
  const [dueOdometer, setDueOdometer] = useState(
    odometerToField(units, reminder?.dueOdometer),
  );
  const [intervalMonths, setIntervalMonths] = useState(
    reminder?.intervalMonths ?? undefined,
  );
  const [intervalKm, setIntervalKm] = useState(
    odometerToField(units, reminder?.intervalKm),
  );
  const [note, setNote] = useState(reminder?.note ?? '');

  const onSuccess = async () => {
    toast.success(t(reminder ? 'reminder.updated' : 'reminder.added'));
    onClose();
    await invalidateCar(carId);
  };
  const onError = (error: Error) =>
    toast.error(errorMessage(error, t('errors.backend')));
  const add = useMutation({
    ...addReminderMutation({ client: localClient }),
    onSuccess,
    onError,
  });
  const update = useMutation({
    ...updateReminderMutation({ client: localClient }),
    onSuccess,
    onError,
  });

  const hasDate = !!dueDate?.isValid();
  const errors = {
    due:
      !hasDate && dueOdometer === undefined
        ? t('reminder.errors.due')
        : undefined,
    intervalMonths:
      intervalMonths !== undefined && !hasDate
        ? t('reminder.errors.intervalMonths')
        : undefined,
    intervalKm:
      intervalKm !== undefined && dueOdometer === undefined
        ? t('reminder.errors.intervalKm')
        : undefined,
  };
  const invalid =
    !title.trim() ||
    Object.values(errors).some(Boolean) ||
    intervalMonths === 0 ||
    intervalKm === 0;

  const applyPreset = (preset: (typeof presets)[number]) => {
    setTitle(t(`reminder.presets.${preset.key}`));
    setIntervalMonths(preset.intervalMonths);
    const interval =
      'intervalKm' in preset
        ? presetInterval(units, preset.intervalKm)
        : undefined;
    setIntervalKm(interval);
    if (!hasDate) {
      setDueDate(dayjs().add(preset.intervalMonths, 'month'));
    }
    if (interval !== undefined && dueOdometer === undefined && odometer) {
      setDueOdometer(odometerToField(units, odometer)! + interval);
    }
  };

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    if (invalid) return;
    const body: ReminderInput = {
      title: title.trim(),
      note: note.trim() || undefined,
      dueDate: hasDate ? toApiDate(dueDate) : undefined,
      dueOdometer: odometerToApi(units, dueOdometer),
      intervalMonths,
      intervalKm: odometerToApi(units, intervalKm),
    };
    if (reminder) {
      update.mutate({ path: { carId, id: reminder.id }, body });
    } else {
      add.mutate({ path: { carId }, body });
    }
  };

  return (
    <Box component="form" onSubmit={submit} noValidate>
      <DialogTitle>
        {t(reminder ? 'reminder.edit.title' : 'reminder.add.title')}
      </DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          {!reminder && (
            <Stack
              direction="row"
              spacing={1}
              useFlexGap
              sx={{
                flexWrap: 'wrap',
              }}
            >
              {presets.map((preset) => (
                <Chip
                  key={preset.key}
                  label={t(`reminder.presets.${preset.key}`)}
                  variant="outlined"
                  onClick={() => applyPreset(preset)}
                />
              ))}
            </Stack>
          )}
          <TextField
            label={t('reminder.fields.title')}
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            required
            autoFocus={!!reminder}
            slotProps={{ htmlInput: { maxLength: 100 } }}
          />

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
              {t('reminder.fields.due')}
            </Typography>
            <Typography
              variant="body2"
              color={errors.due ? 'error' : 'text.secondary'}
              sx={{ mb: 1.5 }}
            >
              {errors.due ?? t('reminder.dueHint')}
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <DatePicker
                label={t('reminder.fields.dueDate')}
                value={dueDate}
                onChange={setDueDate}
                slotProps={{
                  textField: { fullWidth: true },
                  field: { clearable: true },
                }}
              />
              <NumberField
                label={t('reminder.fields.dueOdometer')}
                value={dueOdometer}
                onChange={setDueOdometer}
                decimalScale={0}
                suffix={` ${units.distance.label}`}
                max={10_000_000}
                helperText={
                  odometer !== undefined &&
                  t('reminder.currentOdometer', {
                    km: format.distance(odometer),
                  })
                }
              />
            </Stack>
          </Box>

          <Box>
            <Typography variant="subtitle2" sx={{ mb: 0.5 }}>
              {t('reminder.fields.repeat')}
            </Typography>
            <Typography
              variant="body2"
              sx={{
                color: 'text.secondary',
                mb: 1.5,
              }}
            >
              {t('reminder.repeatHint')}
            </Typography>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <NumberField
                label={t('reminder.fields.intervalMonths')}
                value={intervalMonths}
                onChange={setIntervalMonths}
                decimalScale={0}
                max={120}
                error={errors.intervalMonths}
              />
              <NumberField
                label={t('reminder.fields.intervalKm', {
                  context: units.distance.context,
                })}
                value={intervalKm}
                onChange={setIntervalKm}
                decimalScale={0}
                suffix={` ${units.distance.label}`}
                max={1_000_000}
                error={errors.intervalKm}
              />
            </Stack>
          </Box>

          <TextField
            label={t('reminder.fields.note')}
            value={note}
            onChange={(e) => setNote(e.target.value)}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: 500 } }}
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
