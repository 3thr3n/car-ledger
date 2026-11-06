import { Box, Skeleton, Typography } from '@mui/material';
import ReportProblemRounded from '@mui/icons-material/ReportProblemRounded';
import NotificationsNoneRounded from '@mui/icons-material/NotificationsNoneRounded';
import FactCheckRounded from '@mui/icons-material/FactCheckRounded';
import EventRepeatRounded from '@mui/icons-material/EventRepeatRounded';
import SensorsRounded from '@mui/icons-material/SensorsRounded';
import SensorsOffRounded from '@mui/icons-material/SensorsOffRounded';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import {
  Car,
  DashboardStats,
  MonthStats,
  RecurringBill,
  Reminder,
  ReminderStatus,
} from '@/generated';
import { getTrackersOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { hasValue, num, useFormat } from '@/utils/format';
import { hasTank } from '@/utils/energy';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import { inspectionsOptions } from '@/components/offline/useOfflineDocuments';
import useReminderText from '@/components/reminder/useReminderText';
import { RouterLink } from '@/components/ui/links';
import { scaleOf } from '@/utils/gauge';
import Gauge from './Gauge';
import OdometerWheels from './OdometerWheels';

/** a tracker that sent within this time is online */
const ONLINE_MINUTES = 15;
/** an HU is due soon this many days before the end of its month */
const HU_SOON_DAYS = 30;

type Tone = 'warn' | 'ok' | 'off';

/** a warning light of the cluster: lit when something needs attention */
function StatusLight({
  tone,
  icon,
  title,
  text,
  to,
  carId,
}: {
  tone: Tone;
  icon: ReactNode;
  title: ReactNode;
  text: ReactNode;
  to:
    | '/car/$id/reminders'
    | '/car/$id/inspections'
    | '/car/$id/bill/recurring'
    | '/car/$id/track';
  carId: number;
}) {
  return (
    <Box component="li" sx={{ minWidth: 0 }}>
      <RouterLink
        to={to}
        params={{ id: `${carId}` }}
        underline="none"
        sx={(theme) => ({
          display: 'flex',
          gap: 1.25,
          alignItems: 'center',
          minHeight: 44,
          color: 'text.primary',
          borderRadius: 1,
          '& .lamp': {
            display: 'flex',
            color: 'text.disabled',
            ...(tone === 'warn' && {
              color: theme.vars.palette.warning.main,
              ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
            }),
            ...(tone === 'ok' && {
              color: '#15803d',
              ...theme.applyStyles('dark', { color: '#4ade80' }),
            }),
          },
          '&:hover .title': { textDecoration: 'underline' },
        })}
      >
        {/* not "light": the class of the light color scheme, its colors would
            apply in the dark theme */}
        <span className="lamp" aria-hidden>
          {icon}
        </span>
        <Box component="span" sx={{ minWidth: 0 }}>
          <Typography
            component="span"
            className="title"
            noWrap
            sx={(theme) => ({
              display: 'block',
              fontWeight: 600,
              ...(tone === 'warn' && {
                color: theme.vars.palette.warning.main,
                ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
              }),
            })}
          >
            {title}
          </Typography>
          <Typography
            component="span"
            variant="body2"
            noWrap
            sx={{ display: 'block', color: 'text.secondary' }}
          >
            {text}
          </Typography>
        </Box>
      </RouterLink>
    </Box>
  );
}

function Readout({ label, value }: { label: ReactNode; value: ReactNode }) {
  return (
    <Box sx={{ textAlign: 'center', minWidth: 0 }}>
      <Typography
        component="dt"
        variant="body2"
        sx={{ color: 'text.secondary' }}
      >
        {label}
      </Typography>
      <Typography
        component="dd"
        className="tabular"
        sx={{
          m: 0,
          fontFamily: displayFont,
          fontWeight: 650,
          fontSize: { xs: '1.2rem', sm: '1.375rem' },
          whiteSpace: 'nowrap',
        }}
      >
        {value}
      </Typography>
    </Box>
  );
}

export interface CarClusterProps {
  car: Car;
  /** of the whole time of the car */
  stats?: DashboardStats;
  /** the last twelve months */
  months?: MonthStats[];
  reminders?: Reminder[];
  /** the running recurring costs, the next due first */
  running?: RecurringBill[];
  loading?: boolean;
}

/**
 * The instrument cluster of a car: what it cost, the consumption and the
 * distance a month as dials, the odometer, and the warning lights of its
 * reminders, HU, recurring costs and tracker
 */
export default function CarCluster({
  car,
  stats,
  months = [],
  reminders,
  running,
  loading,
}: CarClusterProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const reminderText = useReminderText();
  const { units } = format;
  const carId = car.id!;
  const { data: inspections } = useQuery(inspectionsOptions(carId));
  const { data: trackers } = useQuery({
    ...getTrackersOptions({ client: localClient, path: { carId } }),
    enabled: car.tracked,
  });

  const total = stats?.total ?? {};
  // null in the JSON without enough readings
  const estimatedToday = car.currentOdometer.estimatedToday ?? undefined;
  // the API sends 0 for an average without entries: shown as missing
  const average = Object.fromEntries(
    Object.entries(stats?.average ?? {}).filter(([, value]) => hasValue(value)),
  ) as NonNullable<DashboardStats['average']>;
  const hiLo = stats?.hiLo ?? {};
  const context = units.distance.context;

  // the consumption of the tank, of the battery for an electric car
  const fuel = hasTank(car.energyType);
  const toConsumption = (value: number) =>
    fuel
      ? units.consumption.fromLitersPer100Km(value)
      : units.chargingConsumption.fromKwhPer100Km(value);
  const consumption = num(
    fuel ? average.fuelConsumption : average.chargingConsumption,
  );
  const range = fuel ? hiLo.consumption : hiLo.chargingConsumption;
  const consumptionRange = hasValue(range?.max) ? range : undefined;
  const lastConsumption = [...months]
    .reverse()
    .map((month) => num(fuel ? month.consumption : month.chargingConsumption))
    .find((value) => value !== undefined);
  const consumptionDigits = fuel ? units.consumption.digits : 2;
  const consumptionValues = [
    consumption,
    num(consumptionRange?.min),
    num(consumptionRange?.max),
    lastConsumption,
  ]
    .filter((value): value is number => value !== undefined)
    .map(toConsumption);
  // mpg turns the lowest consumption into the highest number
  const [consumptionLow, consumptionHigh] = [
    num(consumptionRange?.min),
    num(consumptionRange?.max),
  ]
    .filter((value): value is number => value !== undefined)
    .map(toConsumption)
    .sort((a, b) => a - b);

  // the distance a month from the refuels and charges of the last 12 months
  const driven = months.filter((month) => month.distance > 0);
  const distances = driven.map((month) =>
    units.distance.fromKm(month.distance),
  );
  const perMonth = distances.length
    ? distances.reduce((sum, value) => sum + value, 0) / distances.length
    : undefined;
  const lastMonth = driven.at(-1);
  const costPerMonth = months.length
    ? months.reduce((sum, month) => sum + month.total, 0) / months.length
    : undefined;

  const monthName = (month: string) =>
    new Intl.DateTimeFormat(format.locale, { month: 'short' }).format(
      dayjs(`${month}-01`).toDate(),
    );

  // the most urgent reminder, the next one else
  const reminder =
    reminders?.find((each) => each.status !== ReminderStatus.UPCOMING) ??
    reminders?.[0];
  const latestInspection = inspections?.[0];
  const huDue = latestInspection?.nextDue
    ? dayjs(latestInspection.nextDue).endOf('month')
    : undefined;
  const huDays = huDue?.diff(dayjs(), 'day');
  const payment = running?.[0];
  const tracker = trackers
    ?.filter((each) => !each.revokedAt && each.lastSeenAt)
    .sort((a, b) => dayjs(b.lastSeenAt).diff(dayjs(a.lastSeenAt)))[0];
  const online =
    !!tracker &&
    dayjs().diff(dayjs(tracker.lastSeenAt), 'minute') < ONLINE_MINUTES;

  return (
    <Box
      component="section"
      aria-label={t('car.cluster.label')}
      sx={(theme) => ({
        mb: 3,
        overflow: 'hidden',
        bgcolor: 'background.paper',
        border: 1,
        borderColor: 'divider',
        borderRadius: '10px',
        ...theme.applyStyles('dark', {
          border: 0,
          borderRadius: '28px',
          background: `radial-gradient(120% 90% at 50% 0%, #1c2836 0%, ${theme.vars.palette.background.paper} 70%)`,
          boxShadow: `inset 0 0 0 1px ${theme.vars.palette.divider}`,
        }),
      })}
    >
      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: 'repeat(2, minmax(0, 1fr))',
            md: 'minmax(0, 1fr) minmax(0, 1.3fr) minmax(0, 1fr)',
          },
          gridTemplateAreas: {
            xs: '"center center" "left right"',
            md: '"left center right"',
          },
          alignItems: 'center',
          rowGap: 2,
          columnGap: 2,
          p: { xs: 2.5, sm: 3.5 },
        }}
      >
        <Box sx={{ gridArea: 'left', minWidth: 0 }}>
          {consumption !== undefined ? (
            <Gauge
              {...scaleOf(consumptionValues, 1)}
              value={toConsumption(consumption)}
              low={consumptionLow}
              high={consumptionHigh}
              last={
                lastConsumption !== undefined
                  ? toConsumption(lastConsumption)
                  : undefined
              }
              text={format.number(
                toConsumption(consumption),
                consumptionDigits,
              )}
              unit={
                fuel ? units.consumption.label : units.chargingConsumption.label
              }
              label={t('car.cluster.consumptionLabel', {
                value: fuel
                  ? format.consumption(consumption)
                  : format.chargingConsumption(consumption),
              })}
              caption={
                consumptionLow !== undefined &&
                consumptionHigh !== undefined && (
                  <>
                    <span>
                      {t('car.cluster.low')}{' '}
                      <b>{format.number(consumptionLow, consumptionDigits)}</b>
                    </span>
                    <span>
                      {t('car.cluster.high')}{' '}
                      <b>{format.number(consumptionHigh, consumptionDigits)}</b>
                    </span>
                  </>
                )
              }
            />
          ) : (
            !loading && (
              <Typography sx={{ textAlign: 'center', color: 'text.secondary' }}>
                {t('car.cluster.noConsumption')}
              </Typography>
            )
          )}
        </Box>

        <Box
          sx={{
            gridArea: 'center',
            minWidth: 0,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 1,
          }}
        >
          <Typography sx={{ color: 'text.secondary' }}>
            {t('car.cluster.spent')}
          </Typography>
          <Typography
            component="p"
            className="tabular"
            sx={(theme) => ({
              m: 0,
              fontFamily: displayFont,
              fontWeight: 700,
              fontSize: 'clamp(2.5rem, 1.8rem + 2.6vw, 4rem)',
              lineHeight: 1,
              color: 'text.primary',
              ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
            })}
          >
            {loading ? <Skeleton width={240} /> : format.money(total.total)}
          </Typography>
          <Box
            component="dl"
            sx={{
              m: 0,
              display: 'grid',
              gridTemplateColumns: 'repeat(2, auto)',
              columnGap: 4,
            }}
          >
            <Readout
              label={t('dashboard.summary.costPerKm', { context })}
              value={format.costPerDistance(average.costPerKm)}
            />
            <Readout
              label={t('car.cluster.perMonth')}
              value={format.money(costPerMonth)}
            />
          </Box>
          <OdometerWheels
            value={units.distance.fromKm(car.currentOdometer.value)}
            label={t('car.cluster.odometer', {
              value: format.distance(car.currentOdometer.value),
            })}
          />
          {/* a guess by the average a day, the wheels keep the recorded value */}
          {estimatedToday !== undefined &&
            estimatedToday > car.currentOdometer.value && (
              <Typography
                variant="body2"
                data-testid="odometer-today"
                sx={{ color: 'text.secondary', textAlign: 'center' }}
              >
                {t('car.cluster.estimatedToday', {
                  value: format.distance(estimatedToday),
                  perDay: format.distance(car.currentOdometer.perDay),
                })}
              </Typography>
            )}
        </Box>

        <Box sx={{ gridArea: 'right', minWidth: 0 }}>
          {perMonth !== undefined ? (
            <Gauge
              {...scaleOf(distances, 500)}
              value={perMonth}
              low={Math.min(...distances)}
              high={Math.max(...distances)}
              last={distances.at(-1)}
              text={format.number(perMonth, 0)}
              unit={t('car.cluster.perMonthUnit', {
                unit: units.distance.label,
              })}
              label={t('car.cluster.distanceLabel', {
                value: format.distance(perMonth / units.distance.fromKm(1)),
              })}
              caption={
                lastMonth && (
                  <>
                    <span>
                      {t('car.cluster.low')}{' '}
                      <b>{format.number(Math.min(...distances), 0)}</b>
                    </span>
                    <span>
                      {t('car.cluster.high')}{' '}
                      <b>{format.number(Math.max(...distances), 0)}</b>
                    </span>
                    <span>
                      {monthName(lastMonth.month)}{' '}
                      <b>{format.number(distances.at(-1)!, 0)}</b>
                    </span>
                  </>
                )
              }
            />
          ) : (
            !loading && (
              <Typography sx={{ textAlign: 'center', color: 'text.secondary' }}>
                {t('car.cluster.noDistance')}
              </Typography>
            )
          )}
        </Box>
      </Box>

      <Box
        component="ul"
        aria-label={t('car.cluster.status')}
        sx={(theme) => ({
          listStyle: 'none',
          m: 0,
          px: { xs: 2.5, sm: 3.5 },
          py: 1.25,
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(12rem, 1fr))',
          columnGap: 3,
          rowGap: 0.5,
          borderTop: 1,
          borderColor: 'divider',
          bgcolor: 'action.hover',
          ...theme.applyStyles('dark', { bgcolor: 'rgba(0 0 0 / 0.22)' }),
        })}
      >
        <StatusLight
          carId={carId}
          to="/car/$id/reminders"
          tone={
            reminder && reminder.status !== ReminderStatus.UPCOMING
              ? 'warn'
              : 'off'
          }
          icon={
            reminder && reminder.status !== ReminderStatus.UPCOMING ? (
              <ReportProblemRounded />
            ) : (
              <NotificationsNoneRounded />
            )
          }
          title={reminder ? reminder.title : t('car.overview.reminders')}
          text={
            reminder
              ? (reminderText.dateText(reminder) ??
                reminderText.odometerText(reminder))
              : t('car.overview.remindersEmpty')
          }
        />
        <StatusLight
          carId={carId}
          to="/car/$id/inspections"
          tone={huDays !== undefined && huDays <= HU_SOON_DAYS ? 'warn' : 'off'}
          icon={<FactCheckRounded />}
          title={t('car.cluster.hu')}
          text={
            huDue
              ? new Intl.DateTimeFormat(format.locale, {
                  month: 'long',
                  year: 'numeric',
                }).format(huDue.toDate())
              : t('car.overview.papers.noNextDue')
          }
        />
        <StatusLight
          carId={carId}
          to="/car/$id/bill/recurring"
          tone="off"
          icon={<EventRepeatRounded />}
          title={payment?.name ?? t('car.overview.running')}
          text={
            payment
              ? t('car.cluster.nextPayment', {
                  amount: format.money(payment.amount),
                  date: format.date(payment.nextDueDate),
                })
              : t('car.overview.runningEmpty')
          }
        />
        {car.tracked && (
          <StatusLight
            carId={carId}
            to="/car/$id/track"
            tone={online ? 'ok' : 'off'}
            icon={online ? <SensorsRounded /> : <SensorsOffRounded />}
            title={t(
              online ? 'car.cluster.trackerOnline' : 'car.cluster.tracker',
            )}
            text={
              tracker?.lastSeenAt
                ? t('car.cluster.lastPoint', {
                    date: format.dateTime(tracker.lastSeenAt),
                  })
                : t('car.cluster.noPoint')
            }
          />
        )}
      </Box>
    </Box>
  );
}
