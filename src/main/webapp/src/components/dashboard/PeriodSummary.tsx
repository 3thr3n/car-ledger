import { Box, Skeleton, Typography } from '@mui/material';
import { keyframes } from '@mui/material/styles';
import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { AverageStats, BillType, TotalStats } from '@/generated';
import { billTypes } from '@/utils/billTypes';
import { hasValue, num, useFormat } from '@/utils/format';
import { displayFont } from '@/theme';

interface Part {
  type: BillType;
  amount: number;
  count?: number;
}

/** the band of the split fills once from the left, when the numbers arrive */
const grow = keyframes`
  from { transform: scaleX(0); }
  to { transform: scaleX(1); }
`;

/** the amber of the total on the dark panel, the glow of a cluster */
const AMBER = '#f5a524';

/**
 * Splits the total into the bill types. Recurring payments have no own field,
 * they are the part of the total not covered by the other types.
 */
function costParts(total: TotalStats): { parts: Part[]; sum: number } {
  const fuel = num(total.fuelTotal) ?? 0;
  const charging = num(total.chargingTotal) ?? 0;
  const maintenance = num(total.maintenanceTotal) ?? 0;
  const miscellaneous = num(total.miscellaneousTotal) ?? 0;
  const sum = num(total.total) ?? 0;
  const recurring = Math.max(
    0,
    sum - fuel - charging - maintenance - miscellaneous,
  );
  return {
    sum,
    parts: [
      { type: BillType.FUEL, amount: fuel, count: total.fuelBills },
      // only cars with charges, e.g. not for petrol cars
      ...(total.chargingBills
        ? [
            {
              type: BillType.CHARGING,
              amount: charging,
              count: total.chargingBills,
            },
          ]
        : []),
      {
        type: BillType.MAINTENANCE,
        amount: maintenance,
        count: total.maintenanceBills,
      },
      {
        type: BillType.MISCELLANEOUS,
        amount: miscellaneous,
        count: total.miscellaneousBills,
      },
      { type: BillType.RECURRING, amount: recurring },
    ],
  };
}

/** one figure of the trip computer: a label above its value */
function Readout({
  label,
  value,
  hint,
  loading,
}: {
  label: ReactNode;
  value: ReactNode;
  hint?: ReactNode;
  loading?: boolean;
}) {
  return (
    <Box sx={{ minWidth: 0 }}>
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
          fontSize: { xs: '1.3rem', sm: '1.75rem' },
          lineHeight: 1.2,
        }}
      >
        {loading ? <Skeleton width="70%" /> : value}
      </Typography>
      {hint && !loading && (
        <Typography
          component="dd"
          variant="caption"
          className="tabular"
          sx={{ m: 0, color: 'text.secondary' }}
        >
          {hint}
        </Typography>
      )}
    </Box>
  );
}

/**
 * The instrument panel of the dashboard: what the period cost and where the
 * money went on the left, the figures of the trip computer on the right
 */
export default function PeriodSummary({
  total,
  average,
  loading,
}: {
  total: TotalStats;
  average: AverageStats;
  loading?: boolean;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { parts, sum } = costParts(total);
  const share = (amount: number) => (sum > 0 ? (amount / sum) * 100 : 0);
  const context = format.units.distance.context;
  // the recurring part is a difference of sums: a rest below a cent is none
  const shown = parts.filter((part) => part.amount >= 0.005);

  const refueled = hasValue(total.unit) || hasValue(average.fuelConsumption);
  const charged =
    hasValue(total.chargedUnit) || hasValue(average.chargingConsumption);
  // only charges: the consumption in kWh; with both, kWh is the hint
  const onlyCharged = charged && !refueled;

  return (
    <Box
      component="section"
      aria-label={t('dashboard.summary.label')}
      sx={(theme) => ({
        mb: 3,
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          md: 'minmax(0, 1.2fr) minmax(0, 1fr)',
        },
        overflow: 'hidden',
        bgcolor: 'background.paper',
        border: 1,
        borderColor: 'divider',
        borderRadius: '10px',
        // dark: the lit glass of a cluster, a little brighter at the top
        ...theme.applyStyles('dark', {
          border: 0,
          borderRadius: '24px',
          background: `radial-gradient(120% 100% at 30% 0%, #1c2836 0%, ${theme.vars.palette.background.paper} 70%)`,
          boxShadow: `inset 0 0 0 1px ${theme.vars.palette.divider}`,
        }),
      })}
    >
      <Box sx={{ p: { xs: 2.5, sm: 3.5 }, minWidth: 0 }}>
        <Typography sx={{ color: 'text.secondary' }}>
          {t('dashboard.summary.spent')}
        </Typography>
        <Typography
          component="p"
          className="tabular"
          sx={(theme) => ({
            m: 0,
            fontFamily: displayFont,
            fontWeight: 700,
            fontSize: 'clamp(2.5rem, 1.6rem + 3.2vw, 4rem)',
            lineHeight: 1.1,
            letterSpacing: '-0.01em',
            color: 'text.primary',
            ...theme.applyStyles('dark', { color: AMBER }),
          })}
        >
          {loading ? <Skeleton width="min(20rem, 80%)" /> : format.money(sum)}
        </Typography>

        {!loading && shown.length > 0 && (
          <Box sx={{ mt: 3 }}>
            <Typography variant="subtitle2" component="h2" sx={{ mb: 1.25 }}>
              {t('dashboard.split.title')}
            </Typography>
            <Box
              aria-hidden
              sx={{
                display: 'flex',
                height: 12,
                gap: '3px',
                borderRadius: 1,
                overflow: 'hidden',
                transformOrigin: 'left',
                animation: `${grow} 700ms cubic-bezier(.2,.7,.2,1) both`,
                '@media (prefers-reduced-motion: reduce)': {
                  animation: 'none',
                },
              }}
            >
              {shown.map((part) => (
                <Box
                  key={part.type}
                  sx={{
                    flexGrow: part.amount,
                    flexBasis: 0,
                    minWidth: 6,
                    bgcolor: `${billTypes[part.type].color}.main`,
                  }}
                />
              ))}
            </Box>
            <Box
              component="ul"
              sx={{
                listStyle: 'none',
                p: 0,
                m: 0,
                mt: 1.5,
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(13rem, 1fr))',
                columnGap: 3,
                rowGap: 1,
              }}
            >
              {shown.map((part) => (
                <Box
                  component="li"
                  key={part.type}
                  sx={{
                    display: 'grid',
                    gridTemplateColumns: '10px minmax(0, 1fr) auto',
                    columnGap: 1,
                    alignItems: 'baseline',
                  }}
                >
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: '2px',
                      bgcolor: `${billTypes[part.type].color}.main`,
                    }}
                  />
                  <Typography variant="body2" noWrap>
                    {t(`bill.types.${part.type}`)}
                  </Typography>
                  <Typography
                    variant="body2"
                    className="tabular"
                    sx={{ fontWeight: 600 }}
                  >
                    {format.money(part.amount)}
                  </Typography>
                  <Typography
                    variant="caption"
                    className="tabular"
                    sx={{ gridColumn: 2, color: 'text.secondary' }}
                  >
                    {format.number(share(part.amount), 0)} %
                    {part.count !== undefined &&
                      `, ${t('dashboard.split.count', { count: part.count })}`}
                  </Typography>
                </Box>
              ))}
            </Box>
          </Box>
        )}
      </Box>

      <Box
        component="dl"
        sx={(theme) => ({
          m: 0,
          p: { xs: 2.5, sm: 3.5 },
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          alignContent: 'center',
          gap: { xs: 2.5, sm: 3 },
          borderTop: 1,
          borderColor: 'divider',
          [theme.breakpoints.up('md')]: {
            borderTop: 0,
            borderLeft: 1,
            borderColor: 'divider',
          },
          // dark: the figures on the darker part of the cluster
          ...theme.applyStyles('dark', {
            bgcolor: 'rgba(0 0 0 / 0.18)',
          }),
        })}
      >
        <Readout
          loading={loading}
          label={t('dashboard.summary.costPerKm', { context })}
          value={format.costPerDistance(average.costPerKm)}
        />
        <Readout
          loading={loading}
          label={t('dashboard.summary.distance')}
          value={format.distance(total.trackedDistance)}
        />
        <Readout
          loading={loading}
          label={t('dashboard.summary.consumption')}
          value={
            onlyCharged
              ? format.chargingConsumption(average.chargingConsumption)
              : format.consumption(average.fuelConsumption)
          }
          hint={
            !onlyCharged && hasValue(average.chargingConsumption)
              ? t('dashboard.kpi.chargingConsumption', {
                  value: format.chargingConsumption(
                    average.chargingConsumption,
                  ),
                })
              : undefined
          }
        />
        <Readout
          loading={loading}
          label={t(
            onlyCharged
              ? 'dashboard.summary.charged'
              : 'dashboard.summary.fuel',
          )}
          value={
            onlyCharged
              ? format.unit(total.chargedUnit, 'kWh', 0)
              : format.volume(total.unit, 1)
          }
          hint={
            !onlyCharged && hasValue(total.chargedUnit)
              ? t('dashboard.kpi.chargedVolume', {
                  value: format.unit(total.chargedUnit, 'kWh', 0),
                })
              : undefined
          }
        />
      </Box>
    </Box>
  );
}
