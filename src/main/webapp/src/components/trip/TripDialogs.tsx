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
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Reason, TripConflict, TripType } from '@/generated';
import NumberField from '@/components/bill/NumberField';
import { BackendError, errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import { odometerToApi, odometerToField } from '@/utils/units';
import { TripView } from './tripState';
import { TripResult, useTripActions } from './useTrip';

/** the limits of the API */
const MAX_ODOMETER = 10_000_000;
const MAX_DESTINATION = 200;
const MAX_PURPOSE = 500;
const MAX_BUSINESS_PARTNER = 200;

interface Details {
  destination?: string;
  purpose?: string;
  businessPartner?: string;
}

/** empty fields are not sent, a detail can still be given at the finish */
const trimmed = (details: Details): Details => ({
  destination: details.destination?.trim() || undefined,
  purpose: details.purpose?.trim() || undefined,
  businessPartner: details.businessPartner?.trim() || undefined,
});

/**
 * The odometer in whole units of the account. The lowest value is rounded up,
 * so it is not below the minimum in km after the conversion.
 */
function useOdometer(minimumKm: number) {
  const format = useFormat();
  const { units } = format;
  const minimum = Math.ceil(units.distance.fromKm(minimumKm));
  return {
    units,
    minimum,
    minimumText: format.unit(minimum, units.distance.label, 0),
  };
}

/**
 * The message of a conflict for the odometer field, other errors as toast
 *
 * @return the message for the field, if it belongs there
 */
function useConflict() {
  const { t } = useTranslation();
  const format = useFormat();
  return (error: unknown): string | undefined => {
    const conflict =
      error instanceof BackendError && error.status === 409
        ? (error.body as TripConflict | undefined)
        : undefined;
    if (
      conflict?.reason === Reason.ODOMETER_BELOW_PREVIOUS &&
      conflict.minimumOdometer !== undefined
    ) {
      return t('trip.errors.odometerBelow', {
        minimum: format.distance(conflict.minimumOdometer),
      });
    }
    if (
      conflict?.reason === Reason.ODOMETER_ABOVE_NEXT &&
      conflict.maximumOdometer !== undefined
    ) {
      return t('trip.errors.odometerAbove', {
        maximum: format.distance(conflict.maximumOdometer),
      });
    }
    toast.error(errorMessage(error, t('errors.backend')));
    return undefined;
  };
}

/**
 * The details a business trip needs at the latest at the finish: without them
 * the server rejects the finish, also one sent later from the offline queue
 */
const missingForBusiness = (
  type: TripType,
  details: Details,
): (keyof Details)[] =>
  type === TripType.BUSINESS
    ? (['destination', 'purpose', 'businessPartner'] as const).filter(
        (key) => !details[key]?.trim(),
      )
    : [];

function DetailFields({
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

function useDone(onClose: () => void, savedKey: string) {
  const { t } = useTranslation();
  return (result: TripResult) => {
    if (result === 'queued') toast.info(t('trip.queued'));
    else toast.success(t(savedKey));
    onClose();
  };
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

  const below = value !== undefined && value < odometer.minimum;
  const error =
    serverError ??
    (below
      ? t('trip.errors.odometerBelow', { minimum: odometer.minimumText })
      : undefined);

  const submit = async () => {
    if (value === undefined || below) return;
    setPending(true);
    try {
      const shown = trimmed(details);
      done(
        await start({
          type,
          startOdometer: odometerToApi(units, value),
          ...shown,
          businessPartner:
            type === TripType.BUSINESS ? shown.businessPartner : undefined,
        }),
      );
    } catch (e) {
      setServerError(onConflict(e));
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
    if (value === undefined || below || missing.length) return;
    setPending(true);
    try {
      // only the details missing at the start, the others can't change
      const added = trimmed(details);
      done(
        await finish(trip, {
          endOdometer: odometerToApi(units, value),
          destination: fixed.destination ? undefined : added.destination,
          purpose: fixed.purpose ? undefined : added.purpose,
          businessPartner:
            fixed.businessPartner || trip.type !== TripType.BUSINESS
              ? undefined
              : added.businessPartner,
        }),
      );
    } catch (e) {
      setServerError(onConflict(e));
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
