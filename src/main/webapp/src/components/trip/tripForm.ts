import dayjs, { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Reason, TripConflict, TripType } from '@/generated';
import { BackendError, errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import { TripResult } from './useTrip';

/** The state and the checks of the trip dialogs */

/** the limits of the API */
export const MAX_ODOMETER = 10_000_000;
export const MAX_DESTINATION = 200;
export const MAX_PURPOSE = 500;
export const MAX_BUSINESS_PARTNER = 200;

export interface Details {
  destination?: string;
  purpose?: string;
  businessPartner?: string;
}

/** empty fields are not sent, a detail can still be given at the finish */
export const trimmed = (details: Details): Details => ({
  destination: details.destination?.trim() || undefined,
  purpose: details.purpose?.trim() || undefined,
  businessPartner: details.businessPartner?.trim() || undefined,
});

/**
 * The odometer in whole units of the account. The lowest value is rounded up,
 * so it is not below the minimum in km after the conversion.
 */
export function useOdometer(minimumKm: number) {
  const format = useFormat();
  const { units } = format;
  const minimum = Math.ceil(units.distance.fromKm(minimumKm));
  return {
    units,
    minimum,
    minimumText: format.unit(minimum, units.distance.label, 0),
  };
}

/** messages of a conflict for the odometer fields */
export interface OdometerConflict {
  /** the odometer is below an earlier reading: the start */
  below?: string;
  /** the odometer is above the start of a later trip: the end */
  above?: string;
}

/**
 * The messages of a conflict for the odometer fields, other errors as toast
 *
 * @return the messages for the fields, if they belong there
 */
export function useConflict() {
  const { t } = useTranslation();
  const format = useFormat();
  return (error: unknown): OdometerConflict => {
    const conflict =
      error instanceof BackendError && error.status === 409
        ? (error.body as TripConflict | undefined)
        : undefined;
    if (
      conflict?.reason === Reason.ODOMETER_BELOW_PREVIOUS &&
      conflict.minimumOdometer !== undefined
    ) {
      return {
        below: t('trip.errors.odometerBelow', {
          minimum: format.distance(conflict.minimumOdometer),
        }),
      };
    }
    if (
      conflict?.reason === Reason.ODOMETER_ABOVE_NEXT &&
      conflict.maximumOdometer !== undefined
    ) {
      return {
        above: t('trip.errors.odometerAbove', {
          maximum: format.distance(conflict.maximumOdometer),
        }),
      };
    }
    toast.error(errorMessage(error, t('errors.backend')));
    return {};
  };
}

/**
 * A time of the past, e.g. of a start forgotten for a while: off, the time of
 * the device when it is saved
 *
 * @return the chosen time, undefined while off; an error message
 */
export function useEarlierTime(earliest?: string) {
  const { t } = useTranslation();
  const format = useFormat();
  const [on, setOn] = useState(false);
  const [time, setTime] = useState<Dayjs | null>(() => dayjs());
  let error: string | undefined;
  if (on && !time?.isValid()) error = t('trip.errors.time');
  else if (on && time!.isAfter(dayjs())) error = t('trip.errors.future');
  else if (on && earliest && time!.isBefore(dayjs(earliest)))
    error = t('trip.errors.beforeStart', { time: format.dateTime(earliest) });
  return {
    on,
    setOn,
    time,
    setTime,
    error,
    value: on && time?.isValid() ? time.toISOString() : undefined,
  };
}

/**
 * The details a business trip needs at the latest at the finish: without them
 * the server rejects the finish, also one sent later from the offline queue
 */
export const missingForBusiness = (
  type: TripType,
  details: Details,
): (keyof Details)[] =>
  type === TripType.BUSINESS
    ? (['destination', 'purpose', 'businessPartner'] as const).filter(
        (key) => !details[key]?.trim(),
      )
    : [];

export function useDone(onClose: () => void, savedKey: string) {
  const { t } = useTranslation();
  return (result: TripResult) => {
    if (result === 'queued') toast.info(t('trip.queued'));
    else toast.success(t(savedKey));
    onClose();
  };
}
