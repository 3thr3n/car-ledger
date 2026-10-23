import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers';
import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { TripSuggestion, TripType } from '@/generated';
import NumberField from '@/components/bill/NumberField';
import { isOfflineError } from '@/offline';
import { useFormat } from '@/utils/format';
import { odometerToApi, odometerToField } from '@/utils/units';
import { DetailFields } from './TripDialogs';
import {
  Details,
  MAX_ODOMETER,
  missingForBusiness,
  OdometerConflict,
  trimmed,
  useConflict,
  useDone,
} from './tripForm';
import { useTripActions } from './useTrip';

/**
 * A forgotten trip, entered afterwards with its start and its finish. Only
 * online: the server checks it against the other trips at once, the errors
 * belong into this dialog. The trip log shows it as entered later.
 */
export default function TripPastDialog({
  carId,
  open,
  initial,
  onClose,
}: {
  carId: number;
  open: boolean;
  /** a drive of the GPS tracks, the user confirms or corrects it */
  initial?: TripSuggestion;
  onClose: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {open && <PastForm carId={carId} initial={initial} onClose={onClose} />}
    </Dialog>
  );
}

function PastForm({
  carId,
  initial,
  onClose,
}: {
  carId: number;
  initial?: TripSuggestion;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const { units } = useFormat();
  const [type, setType] = useState<TripType>(TripType.BUSINESS);
  const [startedAt, setStartedAt] = useState<Dayjs | null>(
    initial ? dayjs(initial.startedAt) : null,
  );
  const [endedAt, setEndedAt] = useState<Dayjs | null>(
    initial ? dayjs(initial.endedAt) : null,
  );
  const [start, setStart] = useState(() =>
    odometerToField(units, initial?.startOdometer),
  );
  const [end, setEnd] = useState(() =>
    odometerToField(units, initial?.endOdometer),
  );
  const [details, setDetails] = useState<Details>({});
  const [touched, setTouched] = useState(false);
  const [conflict, setConflict] = useState<OdometerConflict>({});
  const [pending, setPending] = useState(false);
  const { addPast } = useTripActions(carId);
  const onConflict = useConflict();
  const done = useDone(onClose, 'trip.past.added');

  const required = t('trip.errors.required');
  const errors = {
    startedAt: !startedAt?.isValid()
      ? required
      : startedAt.isAfter(dayjs())
        ? t('trip.errors.future')
        : undefined,
    endedAt: !endedAt?.isValid()
      ? required
      : endedAt.isAfter(dayjs())
        ? t('trip.errors.future')
        : startedAt?.isValid() && endedAt.isBefore(startedAt)
          ? t('trip.errors.endBeforeStart')
          : undefined,
    start: start === undefined ? required : conflict.below,
    end:
      end === undefined
        ? required
        : start !== undefined && end < start
          ? t('trip.errors.endBelowStart')
          : conflict.above,
  };
  const missing = missingForBusiness(type, details);
  const invalid = Object.values(errors).some(Boolean) || missing.length > 0;

  const submit = async () => {
    setTouched(true);
    if (invalid) return;
    setPending(true);
    try {
      const shown = trimmed(details);
      done(
        await addPast({
          type,
          startOdometer: odometerToApi(units, start),
          startedAt: startedAt!.toISOString(),
          endOdometer: odometerToApi(units, end),
          endedAt: endedAt!.toISOString(),
          ...shown,
          businessPartner:
            type === TripType.BUSINESS ? shown.businessPartner : undefined,
        }),
      );
    } catch (e) {
      if (isOfflineError(e)) toast.error(t('trip.past.offline'));
      else setConflict(onConflict(e));
    } finally {
      setPending(false);
    }
  };

  const shown = (error?: string) =>
    touched || error !== required ? error : undefined;
  const timeField = (error?: string) => ({
    textField: {
      fullWidth: true,
      required: true,
      error: !!shown(error),
      helperText: shown(error),
    },
  });

  return (
    <>
      <DialogTitle>{t('trip.past.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <Alert severity="info" variant="outlined">
            {initial ? t('trip.suggestions.prefilled') : t('trip.past.text')}
          </Alert>
          <TextField
            select
            label={t('trip.fields.type')}
            value={type}
            onChange={(event) => setType(event.target.value as TripType)}
            fullWidth
          >
            {Object.values(TripType).map((option) => (
              <MenuItem key={option} value={option}>
                {t(`trip.types.${option}`)}
              </MenuItem>
            ))}
          </TextField>
          <DateTimePicker
            label={t('trip.fields.startedAt')}
            value={startedAt}
            onChange={setStartedAt}
            disableFuture
            slotProps={timeField(errors.startedAt)}
          />
          <NumberField
            label={t('trip.fields.startOdometer')}
            value={start}
            onChange={(next) => {
              setStart(next);
              setConflict({});
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            required
            error={shown(errors.start)}
          />
          <DateTimePicker
            label={t('trip.fields.endedAt')}
            value={endedAt}
            onChange={setEndedAt}
            disableFuture
            slotProps={timeField(errors.endedAt)}
          />
          <NumberField
            label={t('trip.fields.endOdometer')}
            value={end}
            onChange={(next) => {
              setEnd(next);
              setConflict({});
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            required
            error={shown(errors.end)}
          />
          <DetailFields
            type={type}
            value={details}
            required
            missing={touched ? missing : []}
            onChange={(patch) => setDetails((d) => ({ ...d, ...patch }))}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={pending}
          onClick={() => void submit()}
        >
          {t('trip.past.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
