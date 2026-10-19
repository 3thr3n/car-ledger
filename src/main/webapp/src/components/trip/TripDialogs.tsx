import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Stack,
  Switch,
  TextField,
} from '@mui/material';
import { DateTimePicker } from '@mui/x-date-pickers';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { TripType } from '@/generated';
import NumberField from '@/components/bill/NumberField';
import { odometerToApi, odometerToField } from '@/utils/units';
import {
  Details,
  MAX_BUSINESS_PARTNER,
  MAX_DESTINATION,
  MAX_ODOMETER,
  MAX_PURPOSE,
  missingForBusiness,
  trimmed,
  useConflict,
  useDone,
  useEarlierTime,
  useOdometer,
} from './tripForm';
import { TripView } from './tripState';
import { useTripActions } from './useTrip';

export function EarlierTime({
  label,
  timeLabel,
  state,
}: {
  label: string;
  timeLabel: string;
  state: ReturnType<typeof useEarlierTime>;
}) {
  return (
    <>
      <FormControlLabel
        control={
          <Switch
            checked={state.on}
            onChange={(event) => state.setOn(event.target.checked)}
          />
        }
        label={label}
      />
      {state.on && (
        <DateTimePicker
          label={timeLabel}
          value={state.time}
          onChange={state.setTime}
          disableFuture
          slotProps={{
            textField: {
              fullWidth: true,
              error: !!state.error,
              helperText: state.error,
            },
          }}
        />
      )}
    </>
  );
}

export function DetailFields({
  type,
  value,
  fixed = {},
  required = false,
  missing = [],
  onChange,
}: {
  type: TripType;
  value: Details;
  /** given at the start, they can't be changed */
  fixed?: Details;
  /** the details of a business trip are needed (at the finish) */
  required?: boolean;
  /** shown as errors */
  missing?: (keyof Details)[];
  onChange: (patch: Details) => void;
}) {
  const { t } = useTranslation();
  const field = (
    key: keyof Details,
    max: number,
    props: { multiline?: boolean } = {},
  ) => (
    <TextField
      label={t(`trip.fields.${key}`)}
      value={fixed[key] ?? value[key] ?? ''}
      onChange={(event) => onChange({ [key]: event.target.value })}
      disabled={!!fixed[key]}
      required={required && type === TripType.BUSINESS && !fixed[key]}
      error={missing.includes(key)}
      helperText={
        missing.includes(key) ? t('trip.errors.businessDetail') : undefined
      }
      slotProps={{ htmlInput: { maxLength: max } }}
      fullWidth
      {...props}
    />
  );
  return (
    <>
      {field('destination', MAX_DESTINATION)}
      {field('purpose', MAX_PURPOSE, { multiline: true })}
      {(type === TripType.BUSINESS || fixed.businessPartner) &&
        field('businessPartner', MAX_BUSINESS_PARTNER)}
    </>
  );
}

export function TripStartDialog({
  carId,
  open,
  minimumOdometer,
  suggestedOdometer,
  onClose,
}: {
  carId: number;
  open: boolean;
  /** in km */
  minimumOdometer: number;
  /** in km */
  suggestedOdometer: number;
  onClose: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {open && (
        <StartForm
          carId={carId}
          minimumOdometer={minimumOdometer}
          suggestedOdometer={suggestedOdometer}
          onClose={onClose}
        />
      )}
    </Dialog>
  );
}

function StartForm({
  carId,
  minimumOdometer,
  suggestedOdometer,
  onClose,
}: {
  carId: number;
  minimumOdometer: number;
  suggestedOdometer: number;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const odometer = useOdometer(minimumOdometer);
  const { units } = odometer;
  const [value, setValue] = useState<number | undefined>(() =>
    Math.max(odometer.minimum, odometerToField(units, suggestedOdometer) ?? 0),
  );
  const [type, setType] = useState<TripType>(TripType.BUSINESS);
  const [details, setDetails] = useState<Details>({});
  const [serverError, setServerError] = useState<string>();
  const [pending, setPending] = useState(false);
  const { start } = useTripActions(carId);
  const onConflict = useConflict();
  const done = useDone(onClose, 'trip.start.started');
  const earlier = useEarlierTime();

  const below = value !== undefined && value < odometer.minimum;
  const error =
    serverError ??
    (below
      ? t('trip.errors.odometerBelow', { minimum: odometer.minimumText })
      : undefined);

  const submit = async () => {
    if (value === undefined || below || earlier.error) return;
    setPending(true);
    try {
      const shown = trimmed(details);
      done(
        await start(
          {
            type,
            startOdometer: odometerToApi(units, value),
            ...shown,
            businessPartner:
              type === TripType.BUSINESS ? shown.businessPartner : undefined,
          },
          earlier.value,
        ),
      );
    } catch (e) {
      const conflict = onConflict(e);
      setServerError(conflict.below ?? conflict.above);
    } finally {
      setPending(false);
    }
  };

  return (
    <>
      <DialogTitle>{t('trip.start.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <NumberField
            label={t('trip.fields.startOdometer')}
            value={value}
            onChange={(next) => {
              setValue(next);
              setServerError(undefined);
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            required
            error={error}
            helperText={t('trip.start.odometerHelp', {
              minimum: odometer.minimumText,
            })}
          />
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
          <DetailFields
            type={type}
            value={details}
            onChange={(patch) => setDetails((d) => ({ ...d, ...patch }))}
          />
          {type === TripType.BUSINESS && (
            <Alert severity="info" variant="outlined">
              {t('trip.start.businessHint')}
            </Alert>
          )}
          <EarlierTime
            label={t('trip.start.earlier')}
            timeLabel={t('trip.fields.startedAt')}
            state={earlier}
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={value === undefined || below || pending}
          onClick={() => void submit()}
        >
          {t('trip.start.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}

export function TripFinishDialog({
  carId,
  trip,
  onClose,
}: {
  carId: number;
  /** the open trip, the dialog is closed without */
  trip?: TripView;
  onClose: () => void;
}) {
  return (
    <Dialog open={!!trip} onClose={onClose} maxWidth="xs" fullWidth>
      {trip && (
        <FinishForm key={trip.id} carId={carId} trip={trip} onClose={onClose} />
      )}
    </Dialog>
  );
}

function FinishForm({
  carId,
  trip,
  onClose,
}: {
  carId: number;
  trip: TripView;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const odometer = useOdometer(trip.startOdometer);
  const { units } = odometer;
  const [value, setValue] = useState<number | undefined>();
  const [details, setDetails] = useState<Details>({});
  const [serverError, setServerError] = useState<string>();
  const [pending, setPending] = useState(false);
  const { finish } = useTripActions(carId);
  const onConflict = useConflict();
  const done = useDone(onClose, 'trip.finish.finished');
  const earlier = useEarlierTime(trip.startedAt);
  const fixed: Details = {
    destination: trip.destination,
    purpose: trip.purpose,
    businessPartner: trip.businessPartner,
  };

  const below = value !== undefined && value < odometer.minimum;
  const error =
    serverError ??
    (below
      ? t('trip.errors.odometerBelow', { minimum: odometer.minimumText })
      : undefined);
  const [touched, setTouched] = useState(false);
  const missing = missingForBusiness(trip.type, {
    destination: fixed.destination ?? details.destination,
    purpose: fixed.purpose ?? details.purpose,
    businessPartner: fixed.businessPartner ?? details.businessPartner,
  });

  const submit = async () => {
    setTouched(true);
    if (value === undefined || below || missing.length || earlier.error) {
      return;
    }
    setPending(true);
    try {
      // only the details missing at the start, the others can't change
      const added = trimmed(details);
      done(
        await finish(
          trip,
          {
            endOdometer: odometerToApi(units, value),
            destination: fixed.destination ? undefined : added.destination,
            purpose: fixed.purpose ? undefined : added.purpose,
            businessPartner:
              fixed.businessPartner || trip.type !== TripType.BUSINESS
                ? undefined
                : added.businessPartner,
          },
          earlier.value,
        ),
      );
    } catch (e) {
      const conflict = onConflict(e);
      setServerError(conflict.below ?? conflict.above);
    } finally {
      setPending(false);
    }
  };

  return (
    <>
      <DialogTitle>{t('trip.finish.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <NumberField
            label={t('trip.fields.endOdometer')}
            value={value}
            onChange={(next) => {
              setValue(next);
              setServerError(undefined);
            }}
            decimalScale={0}
            suffix={` ${units.distance.label}`}
            max={MAX_ODOMETER}
            required
            error={error}
            helperText={t('trip.finish.odometerHelp', {
              minimum: odometer.minimumText,
            })}
          />
          <DetailFields
            type={trip.type}
            value={details}
            fixed={fixed}
            required
            missing={touched ? missing : []}
            onChange={(patch) => setDetails((d) => ({ ...d, ...patch }))}
          />
          <EarlierTime
            label={t('trip.finish.earlier')}
            timeLabel={t('trip.fields.endedAt')}
            state={earlier}
          />
          <Alert severity="warning" variant="outlined">
            {t('trip.finish.final')}
          </Alert>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={value === undefined || below || pending}
          onClick={() => void submit()}
        >
          {t('trip.finish.confirm')}
        </Button>
      </DialogActions>
    </>
  );
}
