import { Box, Skeleton, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { BillType, DashboardStats } from '@/generated';
import { getDashboardStatsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { PumpReading, PumpReadings } from './Pump';
import { hasValue, useFormat } from '@/utils/format';
import { CLUSTER_AMBER, displayFont } from '@/theme';

export type SummaryType =
  | BillType.FUEL
  | BillType.CHARGING
  | BillType.MAINTENANCE
  | BillType.MISCELLANEOUS;

/**
 * What the entries of the list cost in the year shown, or in all years: for
 * fuel and charging like the display of a pump (paid, the amount, the price
 * of a unit), with the averages in a sentence; for the others the total with
 * the count and the average in a sentence
 */
export default function CostSummary({
  carId,
  type,
  year,
}: {
  carId: number;
  type: SummaryType;
  /** none for all years */
  year?: number;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const { data: stats, isLoading } = useQuery(
    getDashboardStatsOptions({
      client: localClient,
      path: { carId },
      query:
        year === undefined
          ? undefined
          : { from: `${year}-01-01`, to: `${year}-12-31` },
    }),
  );

  const total: NonNullable<DashboardStats['total']> = stats?.total ?? {};
  // the API sends 0 for an average without entries: left out
  const average = Object.fromEntries(
    Object.entries(stats?.average ?? {}).filter(([, value]) => hasValue(value)),
  ) as NonNullable<DashboardStats['average']>;

  const count = {
    [BillType.FUEL]: total.fuelBills,
    [BillType.CHARGING]: total.chargingBills,
    [BillType.MAINTENANCE]: total.maintenanceBills,
    [BillType.MISCELLANEOUS]: total.miscellaneousBills,
  }[type];
  const paid = {
    [BillType.FUEL]: total.fuelTotal,
    [BillType.CHARGING]: total.chargingTotal,
    [BillType.MAINTENANCE]: total.maintenanceTotal,
    [BillType.MISCELLANEOUS]: total.miscellaneousTotal,
  }[type];

  if (isLoading) {
    return (
      <Box sx={{ px: 2.5, pb: 2 }}>
        <Skeleton variant="rounded" height={96} sx={{ maxWidth: 420 }} />
      </Box>
    );
  }
  // nothing to sum up: the list shows how to add the first
  if (!count) return null;

  const period =
    year === undefined
      ? t('bill.summary.allYears')
      : t('bill.summary.year', { year });

  // the averages as a sentence, the ones there are
  const sentence = (parts: (string | undefined)[]) => (
    <Typography sx={{ color: 'text.secondary', maxWidth: '36rem' }}>
      {parts.filter(Boolean).join(', ')}.
    </Typography>
  );

  if (type === BillType.FUEL || type === BillType.CHARGING) {
    const fuel = type === BillType.FUEL;
    return (
      <Box
        component="section"
        aria-label={t('bill.summary.label')}
        data-testid="cost-summary"
        sx={{
          px: 2.5,
          pb: 2.5,
          display: 'grid',
          gridTemplateColumns: { xs: 'minmax(0, 1fr)', md: '26rem 1fr' },
          columnGap: 4,
          rowGap: 1.5,
          alignItems: 'center',
        }}
      >
        <PumpReadings>
          <PumpReading
            label={t('bill.summary.paid')}
            value={format.money(paid)}
          />
          <PumpReading
            label={
              fuel
                ? t('bill.fields.unit', {
                    context: format.units.volume.context,
                  })
                : t('bill.fields.chargedUnit')
            }
            value={
              fuel
                ? format.volume(total.unit, 1)
                : format.unit(total.chargedUnit, 'kWh', 1)
            }
          />
          <PumpReading
            label={
              fuel
                ? t('bill.summary.averagePrice', {
                    context: format.units.volume.context,
                  })
                : t('bill.summary.averageChargingPrice')
            }
            value={
              fuel
                ? format.fuelPrice(average.pricePerUnit)
                : format.unit(average.chargingPricePerUnit, 'ct/kWh', 1)
            }
          />
        </PumpReadings>
        <Box>
          <Typography
            sx={{ fontFamily: displayFont, fontWeight: 650, mb: 0.25 }}
          >
            {period}
          </Typography>
          {sentence(
            fuel
              ? [
                  t('bill.summary.refuels', { count }),
                  hasValue(average.fuelConsumption)
                    ? t('bill.summary.consumption', {
                        value: format.consumption(average.fuelConsumption),
                      })
                    : undefined,
                  hasValue(average.distance)
                    ? t('bill.summary.distance', {
                        value: format.distance(average.distance),
                      })
                    : undefined,
                ]
              : [
                  t('bill.summary.charges', { count }),
                  hasValue(average.chargingConsumption)
                    ? t('bill.summary.consumption', {
                        value: format.chargingConsumption(
                          average.chargingConsumption,
                        ),
                      })
                    : undefined,
                ],
          )}
        </Box>
      </Box>
    );
  }

  return (
    <Box
      component="section"
      aria-label={t('bill.summary.label')}
      data-testid="cost-summary"
      sx={{ px: 2.5, pt: 1, pb: 2.5 }}
    >
      <Typography
        className="tabular"
        sx={(theme) => ({
          fontFamily: displayFont,
          fontWeight: 700,
          fontSize: { xs: '2.25rem', sm: '2.75rem' },
          lineHeight: 1.05,
          ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
        })}
      >
        {format.money(paid)}
      </Typography>
      <Typography sx={{ color: 'text.secondary', maxWidth: '36rem' }}>
        {t('bill.summary.costs', {
          period,
          count,
          context: type === BillType.MAINTENANCE ? 'maintenance' : undefined,
        })}
        {paid && count > 1
          ? `, ${t('bill.summary.average', { value: format.money(paid / count) })}`
          : ''}
        .
      </Typography>
    </Box>
  );
}
