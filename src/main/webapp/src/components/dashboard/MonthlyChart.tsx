import {
  Box,
  ToggleButton,
  ToggleButtonGroup,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import BarChartRounded from '@mui/icons-material/BarChartRounded';
import { BarChart } from '@mui/x-charts/BarChart';
import { LineChart } from '@mui/x-charts/LineChart';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, MonthStats } from '@/generated';
import { getMonthlyStatsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { billTypes } from '@/utils/billTypes';
import { Formatter, num, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';

export interface MonthlyChartProps {
  /** -1 for all cars */
  carId: number;
  from?: string;
  to?: string;
  subtitle?: string;
}

type View = 'costs' | 'fuel' | 'charging';

/**
 * The price and consumption lines of the refuels or the charges, in the units
 * of the account
 */
function energyViews(format: Formatter) {
  const { units } = format;
  const fuelPriceLabel = units.fuelPrice.inCurrency
    ? `${format.currencySymbol}/${units.volume.label}`
    : `ct/${units.volume.label}`;
  return {
    fuel: {
      price: 'pricePerUnit',
      consumption: 'consumption',
      priceLabel: fuelPriceLabel,
      consumptionLabel: units.consumption.label,
      toPrice: units.fuelPrice.fromCentsPerLiter,
      toConsumption: units.consumption.fromLitersPer100Km,
      formatPrice: (value: number | null) =>
        units.fuelPrice.inCurrency
          ? value === null
            ? null
            : `${format.money(value, units.fuelPrice.digits)}/${units.volume.label}`
          : format.unit(value, fuelPriceLabel, units.fuelPrice.digits),
      formatConsumption: (value: number | null) =>
        format.unit(value, units.consumption.label, units.consumption.digits),
      color: 'fuel',
    },
    charging: {
      price: 'chargingPricePerUnit',
      consumption: 'chargingConsumption',
      priceLabel: 'ct/kWh',
      consumptionLabel: units.chargingConsumption.label,
      toPrice: (value: number) => value,
      toConsumption: units.chargingConsumption.fromKwhPer100Km,
      formatPrice: (value: number | null) => format.unit(value, 'ct/kWh', 1),
      formatConsumption: (value: number | null) =>
        format.unit(value, units.chargingConsumption.label),
      color: 'charging',
    },
  } as const;
}

const HEIGHT = 300;

const costParts = [
  { key: 'fuel', type: BillType.FUEL },
  { key: 'charging', type: BillType.CHARGING },
  { key: 'maintenance', type: BillType.MAINTENANCE },
  { key: 'miscellaneous', type: BillType.MISCELLANEOUS },
  { key: 'recurring', type: BillType.RECURRING },
] as const;

/** a missing average is a gap in the line, not a zero */
const orNull = (value?: number | null) => num(value) ?? null;

/** a value of the API in the units of the account, missing stays a gap */
const inUnits = (
  value: number | null | undefined,
  conversion: (value: number) => number,
) => {
  const number = orNull(value);
  return number === null ? null : conversion(number);
};

/**
 * Costs per month by type, or price and consumption of the refuels or the
 * charges per month.
 */
export default function MonthlyChart({
  carId,
  from,
  to,
  subtitle,
}: MonthlyChartProps) {
  const { t } = useTranslation();
  const format = useFormat();
  const theme = useTheme();
  const isPhone = useMediaQuery(theme.breakpoints.down('sm'));
  const [view, setView] = useState<View>('costs');

  const { data: months = [], isLoading } = useQuery(
    getMonthlyStatsOptions({
      client: localClient,
      path: { carId },
      query: { from, to },
    }),
  );

  const label = (month: string, long = false) =>
    new Intl.DateTimeFormat(format.locale, {
      month: long ? 'long' : 'short',
      year: long ? 'numeric' : '2-digit',
    }).format(dayjs(`${month}-01`).toDate());

  // not every month gets a label, they would overlap
  const step = Math.max(1, Math.ceil(months.length / (isPhone ? 4 : 12)));
  const xAxis = {
    scaleType: 'band' as const,
    dataKey: 'month',
    valueFormatter: (month: string, context: { location: string }) =>
      label(month, context.location === 'tooltip'),
    tickLabelInterval: (_: unknown, index: number) => index % step === 0,
  };

  // the tooltip of the costs shows the total of the month above the split
  const totals = new Map(
    months.map((month) => [month.month, num(month.total) ?? 0]),
  );
  const costsXAxis = {
    ...xAxis,
    valueFormatter: (month: string, context: { location: string }) =>
      context.location === 'tooltip'
        ? t('dashboard.monthly.tooltipTotal', {
            month: label(month, true),
            total: format.money(totals.get(month)),
          })
        : label(month),
  };

  const hasCosts = months.some((month) => (num(month.total) ?? 0) > 0);
  const hasFuel = months.some((month) => num(month.pricePerUnit) !== undefined);
  const hasCharging = months.some(
    (month) => num(month.chargingPricePerUnit) !== undefined,
  );
  // the fuel view stays without any charges, also when it is still empty
  const views: View[] = [
    'costs',
    ...(hasFuel || !hasCharging ? (['fuel'] as const) : []),
    ...(hasCharging ? (['charging'] as const) : []),
  ];
  // e.g. the fuel view of a filter which now only has charges
  const shown = views.includes(view) ? view : 'costs';
  const isEmpty =
    shown === 'costs' ? !hasCosts : shown === 'fuel' ? !hasFuel : !hasCharging;
  const energy = shown === 'costs' ? undefined : energyViews(format)[shown];

  const compactMoney = new Intl.NumberFormat(format.locale, {
    style: 'currency',
    currency: format.currency,
    notation: 'compact',
  });

  return (
    <Section
      title={t('dashboard.monthly.title')}
      subtitle={subtitle}
      action={
        <ToggleButtonGroup
          size="small"
          exclusive
          value={shown}
          onChange={(_, value) => value && setView(value)}
        >
          {views.map((option) => (
            <ToggleButton key={option} value={option} sx={{ px: 1.5 }}>
              {t(`dashboard.monthly.${option}`)}
            </ToggleButton>
          ))}
        </ToggleButtonGroup>
      }
    >
      {!isLoading && isEmpty ? (
        <Box
          sx={{
            height: HEIGHT,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          <EmptyState
            compact
            icon={<BarChartRounded />}
            title={t(`dashboard.monthly.empty.${shown}`)}
          />
        </Box>
      ) : !energy ? (
        <BarChart
          height={HEIGHT}
          loading={isLoading}
          dataset={months.map(costRow)}
          xAxis={[costsXAxis]}
          yAxis={[
            {
              width: 64,
              valueFormatter: (value: number) => compactMoney.format(value),
            },
          ]}
          series={costParts.map((part) => ({
            dataKey: part.key,
            stack: 'total',
            label: t(`bill.types.${part.type}`),
            color: theme.palette[billTypes[part.type].color].main,
            valueFormatter: (value: number | null) =>
              value ? format.money(value) : null,
          }))}
          margin={{ left: 0, right: 8 }}
        />
      ) : (
        <LineChart
          height={HEIGHT}
          loading={isLoading}
          dataset={months.map((month) => ({
            month: month.month,
            pricePerUnit: inUnits(month[energy.price], energy.toPrice),
            consumption: inUnits(
              month[energy.consumption],
              energy.toConsumption,
            ),
          }))}
          xAxis={[xAxis]}
          yAxis={[
            {
              id: 'price',
              width: 56,
              label: energy.priceLabel,
            },
            {
              id: 'consumption',
              position: 'right',
              width: 48,
              label: energy.consumptionLabel,
            },
          ]}
          series={[
            {
              dataKey: 'pricePerUnit',
              yAxisId: 'price',
              label: t(`dashboard.monthly.pricePerUnit.${shown}`),
              color: theme.palette[energy.color].main,
              connectNulls: true,
              valueFormatter: energy.formatPrice,
            },
            {
              dataKey: 'consumption',
              yAxisId: 'consumption',
              label: t('dashboard.monthly.consumption'),
              color: theme.palette.primary.main,
              connectNulls: true,
              valueFormatter: energy.formatConsumption,
            },
          ]}
          margin={{ left: 0, right: 0 }}
        />
      )}
    </Section>
  );
}

function costRow(month: MonthStats) {
  return {
    month: month.month,
    fuel: num(month.fuel) ?? 0,
    charging: num(month.charging) ?? 0,
    maintenance: num(month.maintenance) ?? 0,
    miscellaneous: num(month.miscellaneous) ?? 0,
    recurring: num(month.recurring) ?? 0,
  };
}
