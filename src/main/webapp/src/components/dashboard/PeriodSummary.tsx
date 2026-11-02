import { Box, Skeleton, Stack, Typography } from '@mui/material';
import { keyframes } from '@mui/material/styles';
import { Trans, useTranslation } from 'react-i18next';
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

/**
 * The answer of the dashboard: what the period cost, per distance, and where
 * the money went
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

  const fuelVolume = format.volume(total.unit, 1);
  const chargedVolume = format.unit(total.chargedUnit, 'kWh', 0);
  const volumes = hasValue(total.chargedUnit)
    ? hasValue(total.unit)
      ? t('dashboard.kpi.bothVolumes', {
          fuel: fuelVolume,
          charged: chargedVolume,
        })
      : t('dashboard.kpi.chargedVolume', { value: chargedVolume })
    : hasValue(total.unit)
      ? t('dashboard.kpi.fuelVolume', { value: fuelVolume })
      : undefined;
  // only charges: the consumption is in kWh, with both kWh is the addition
  const onlyCharged =
    hasValue(average.chargingConsumption) && !hasValue(average.fuelConsumption);
  const consumption = onlyCharged
    ? format.chargingConsumption(average.chargingConsumption)
    : hasValue(average.fuelConsumption)
      ? format.consumption(average.fuelConsumption)
      : undefined;
  const usage = [
    consumption && t('dashboard.summary.consumption', { value: consumption }),
    !onlyCharged &&
      hasValue(average.chargingConsumption) &&
      t('dashboard.kpi.chargingConsumption', {
        value: format.chargingConsumption(average.chargingConsumption),
      }),
    volumes,
  ].filter(Boolean);

  const figure = { fontWeight: 650, color: 'text.primary' };
  // the recurring part is a difference of sums: a rest below a cent is none
  const shown = parts.filter((part) => part.amount >= 0.005);

  return (
    <Box
      component="section"
      aria-label={t('dashboard.summary.label')}
      sx={{ mt: 4, mb: 4 }}
    >
      <Typography
        component="p"
        className="tabular"
        sx={{
          fontFamily: displayFont,
          fontWeight: 300,
          fontSize: 'clamp(2.75rem, 1.6rem + 4.4vw, 4.75rem)',
          lineHeight: 1.05,
          letterSpacing: '-0.025em',
          color: 'text.secondary',
          '& b': { ...figure, fontWeight: 700 },
        }}
      >
        {loading ? (
          <Skeleton width="min(28rem, 80%)" />
        ) : (
          <Trans
            i18nKey="dashboard.summary.spent"
            values={{ total: format.money(total.total) }}
            components={{ b: <b /> }}
          />
        )}
      </Typography>

      {(loading || hasValue(total.trackedDistance)) && (
        <Typography
          component="p"
          className="tabular"
          sx={{
            fontFamily: displayFont,
            fontSize: 'clamp(1.25rem, 1rem + 0.9vw, 1.75rem)',
            lineHeight: 1.3,
            color: 'text.secondary',
            mt: 1,
            '& b': figure,
          }}
        >
          {loading ? (
            <Skeleton width="min(22rem, 70%)" />
          ) : (
            <Trans
              i18nKey={
                hasValue(average.costPerKm)
                  ? 'dashboard.summary.perDistance'
                  : 'dashboard.summary.distance'
              }
              values={{
                cost: format.costPerDistance(average.costPerKm),
                distance: format.distance(total.trackedDistance),
              }}
              components={{ b: <b /> }}
            />
          )}
        </Typography>
      )}

      {!loading && usage.length > 0 && (
        <Typography
          sx={{ color: 'text.secondary', mt: 1.5 }}
          className="tabular"
        >
          {usage.join(', ')}
        </Typography>
      )}

      {!loading && shown.length > 0 && (
        <Box sx={{ mt: 4, maxWidth: 960 }}>
          <Typography variant="h6" component="h2" sx={{ mb: 1.5 }}>
            {t('dashboard.split.title')}
          </Typography>
          <Box
            aria-hidden
            sx={{
              display: 'flex',
              height: 28,
              gap: '3px',
              borderRadius: 1.5,
              overflow: 'hidden',
              transformOrigin: 'left',
              animation: `${grow} 700ms cubic-bezier(.2,.7,.2,1) both`,
              '@media (prefers-reduced-motion: reduce)': { animation: 'none' },
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
              mt: 2,
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fill, minmax(9rem, 1fr))',
              columnGap: 3,
              rowGap: 2,
            }}
          >
            {shown.map((part) => (
              <Stack
                component="li"
                key={part.type}
                direction="row"
                spacing={1.25}
                sx={{ alignItems: 'baseline' }}
              >
                <Box
                  sx={{
                    width: 10,
                    height: 10,
                    borderRadius: '2px',
                    flexShrink: 0,
                    bgcolor: `${billTypes[part.type].color}.main`,
                  }}
                />
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="body2">
                    {t(`bill.types.${part.type}`)}
                  </Typography>
                  <Typography
                    className="tabular"
                    sx={{
                      fontFamily: displayFont,
                      fontWeight: 650,
                      fontSize: '1.25rem',
                      lineHeight: 1.3,
                    }}
                  >
                    {format.money(part.amount)}
                  </Typography>
                  <Typography
                    variant="caption"
                    component="p"
                    className="tabular"
                    sx={{ color: 'text.secondary' }}
                  >
                    {format.number(share(part.amount), 0)} %
                    {part.count !== undefined &&
                      `, ${t('dashboard.split.count', { count: part.count })}`}
                  </Typography>
                </Box>
              </Stack>
            ))}
          </Box>
        </Box>
      )}
    </Box>
  );
}
