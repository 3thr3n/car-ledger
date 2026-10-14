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
import { num, useFormat } from '@/utils/format';
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

/** the price and consumption lines of the refuels or the charges */
const energyViews = {
  fuel: {
    price: 'pricePerUnit',
    consumption: 'consumption',
    priceUnit: 'ct/l',
    consumptionUnit: 'l/100km',
    color: 'fuel',
  },
  charging: {
    price: 'chargingPricePerUnit',
    consumption: 'chargingConsumption',
    priceUnit: 'ct/kWh',
    consumptionUnit: 'kWh/100km',
    color: 'charging',
  },
} as const;

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
  const energy = shown === 'costs' ? undefined : energyViews[shown];

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
            pricePerUnit: orNull(month[energy.price]),
            consumption: orNull(month[energy.consumption]),
          }))}
          xAxis={[xAxis]}
          yAxis={[
            {
              id: 'price',
              width: 56,
              label: energy.priceUnit,
            },
            {
              id: 'consumption',
              position: 'right',
              width: 48,
              label: energy.consumptionUnit,
            },
          ]}
          series={[
            {
              dataKey: 'pricePerUnit',
              yAxisId: 'price',
              label: t(`dashboard.monthly.pricePerUnit.${shown}`),
              color: theme.palette[energy.color].main,
              connectNulls: true,
              valueFormatter: (value: number | null) =>
                format.unit(value, energy.priceUnit, 1),
            },
            {
              dataKey: 'consumption',
              yAxisId: 'consumption',
              label: t('dashboard.monthly.consumption'),
              color: theme.palette.primary.main,
              connectNulls: true,
              valueFormatter: (value: number | null) =>
                format.unit(value, energy.consumptionUnit),
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
