import { Box, Skeleton, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Car, DashboardStats } from '@/generated';
import { hasValue, num, useFormat } from '@/utils/format';
import { hasBattery, hasTank } from '@/utils/energy';
import { displayFont } from '@/theme';
import Section from '@/components/ui/Section';

interface Row {
  key: string;
  label: ReactNode;
  value: ReactNode;
}

/**
 * The averages of a car over all its entries, like the trip computer of the
 * car: prices, distances, costs per km and the range of a full tank or battery
 */
export default function TripComputer({
  car,
  stats,
  loading,
}: {
  car: Car;
  stats?: DashboardStats;
  loading?: boolean;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const context = format.units.distance.context;
  const total = stats?.total ?? {};
  // the API sends 0 for an average without entries: shown as missing
  const average = Object.fromEntries(
    Object.entries(stats?.average ?? {}).filter(([, value]) => hasValue(value)),
  ) as NonNullable<DashboardStats['average']>;

  // a full tank (battery) at the average consumption, in km of the API
  const range = (size?: number, per100Km?: number | string) => {
    const consumption = num(per100Km);
    return size && consumption ? (size / consumption) * 100 : undefined;
  };

  const rows: Row[] = [
    ...(hasTank(car.energyType)
      ? [
          {
            key: 'fuelPrice',
            label: t('dashboard.average.pricePerUnit'),
            value: format.fuelPrice(average.pricePerUnit),
          },
          {
            key: 'rangePerTank',
            label: t('car.cluster.rangePerTank'),
            value: format.distance(
              range(car.tankSize, average.fuelConsumption),
            ),
          },
          {
            key: 'distancePerRefuel',
            label: t('dashboard.average.distance'),
            value: format.distance(average.distance),
          },
          {
            key: 'fuelCostPerKm',
            label: t('dashboard.average.fuelCostPerKm', { context }),
            value: format.costPerDistance(average.fuelCostPerKm),
          },
          {
            key: 'refuels',
            label: t('car.overview.refuels'),
            value: format.number(total.fuelBills ?? 0, 0),
          },
          {
            key: 'fuelTanked',
            label: t('dashboard.summary.fuel'),
            value: format.volume(total.unit, 1),
          },
        ]
      : []),
    ...(hasBattery(car.energyType)
      ? [
          {
            key: 'chargingPrice',
            label: t('dashboard.average.chargingPricePerUnit'),
            value: format.unit(average.chargingPricePerUnit, 'ct/kWh', 1),
          },
          {
            key: 'rangePerBattery',
            label: t('car.cluster.rangePerBattery'),
            value: format.distance(
              range(car.batteryCapacity, average.chargingConsumption),
            ),
          },
          {
            key: 'chargingCostPerKm',
            label: t('dashboard.average.chargingCostPerKm', { context }),
            value: format.costPerDistance(average.chargingCostPerKm),
          },
          {
            key: 'charges',
            label: t('car.overview.charges'),
            value: format.number(total.chargingBills ?? 0, 0),
          },
          {
            key: 'charged',
            label: t('dashboard.summary.charged'),
            value: format.unit(total.chargedUnit, 'kWh', 0),
          },
        ]
      : []),
    {
      key: 'maintenanceCost',
      label: t('dashboard.average.maintenanceCost'),
      value: format.money(average.maintenanceCost),
    },
  ];

  return (
    <Section title={t('car.cluster.tripComputer')} sx={{ height: '100%' }}>
      <Box
        component="dl"
        sx={{
          m: 0,
          display: 'grid',
          gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
          gap: 2.25,
        }}
      >
        {rows.map((row) => (
          <Box key={row.key} sx={{ minWidth: 0 }}>
            <Typography
              component="dt"
              variant="body2"
              sx={{ color: 'text.secondary' }}
            >
              {row.label}
            </Typography>
            <Typography
              component="dd"
              className="tabular"
              sx={{
                m: 0,
                fontFamily: displayFont,
                fontWeight: 650,
                fontSize: '1.375rem',
                lineHeight: 1.25,
              }}
            >
              {loading ? <Skeleton width="70%" /> : row.value}
            </Typography>
          </Box>
        ))}
      </Box>
    </Section>
  );
}
