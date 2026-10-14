import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { HiLo, HiLoStats } from '@/generated';
import { EMPTY, Formatter, hasValue, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';

interface Row {
  key: keyof HiLoStats;
  /** a value of the API in the units of the account */
  format: (format: Formatter, value: number) => string;
  /** only shown with refuels or with charges */
  energy?: 'fuel' | 'charging';
  /** the lowest value of the API is the highest one shown, e.g. mpg */
  inverse?: (format: Formatter) => boolean;
}

const money = (format: Formatter, value: number) => format.money(value);

const rows: Row[] = [
  {
    key: 'pricePerUnit',
    format: (f, value) => f.fuelPrice(value),
    energy: 'fuel',
  },
  {
    key: 'consumption',
    format: (f, value) => f.consumption(value),
    energy: 'fuel',
    inverse: (f) => f.units.consumption.label === 'mpg',
  },
  {
    key: 'fuelCostPerKm',
    format: (f, value) => f.costPerDistance(value),
    energy: 'fuel',
  },
  { key: 'distance', format: (f, value) => f.distance(value), energy: 'fuel' },
  { key: 'fuelCost', format: money, energy: 'fuel' },
  {
    key: 'chargingPricePerUnit',
    format: (f, value) => f.unit(value, 'ct/kWh', 1),
    energy: 'charging',
  },
  {
    key: 'chargingConsumption',
    format: (f, value) => f.chargingConsumption(value),
    energy: 'charging',
  },
  { key: 'chargingCost', format: money, energy: 'charging' },
  { key: 'maintenanceCost', format: money },
];

/**
 * Lowest and highest value per metric, as reported by the API
 */
export default function RangeList({ hiLo }: { hiLo: HiLoStats }) {
  const { t } = useTranslation();
  const format = useFormat();
  const charged = hasValue(hiLo.chargingPricePerUnit?.max);
  // without any charges the fuel rows stay, also when they are still empty
  const refueled = hasValue(hiLo.pricePerUnit?.max) || !charged;
  const shown = rows.filter(
    (row) =>
      !row.energy ||
      (row.energy === 'fuel' && refueled) ||
      (row.energy === 'charging' && charged),
  );

  const value = (range: HiLo | undefined, side: 'min' | 'max', row: Row) => {
    const inverse = row.inverse?.(format);
    const number = range?.[inverse ? (side === 'min' ? 'max' : 'min') : side];
    if (number === undefined || number === null) return EMPTY;
    return row.format(format, number);
  };

  return (
    <Section
      title={t('dashboard.range.title')}
      subtitle={t('dashboard.range.subtitle')}
    >
      <Stack spacing={2}>
        {shown.map((row) => {
          const { key } = row;
          const range = hiLo[key];
          const hasRange = range?.min !== undefined && hasValue(range?.max);

          return (
            <Box key={key}>
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                  mb: 0.5,
                }}
              >
                {t(`dashboard.range.${key}`, {
                  context: format.units.distance.context,
                })}
              </Typography>
              <Stack
                direction="row"
                spacing={1.5}
                sx={{
                  alignItems: 'center',
                }}
              >
                <Typography
                  variant="body2"
                  className="tabular"
                  sx={{
                    fontWeight: 600,
                    minWidth: 92,
                  }}
                >
                  {value(range, 'min', row)}
                </Typography>
                <Box
                  sx={{
                    flexGrow: 1,
                    height: 6,
                    borderRadius: 999,
                    background: (theme) =>
                      hasRange
                        ? `linear-gradient(90deg, rgba(${theme.vars.palette.primary.mainChannel} / 0.25), ${theme.vars.palette.primary.main})`
                        : theme.vars.palette.action.hover,
                  }}
                />
                <Typography
                  variant="body2"
                  className="tabular"
                  sx={{
                    fontWeight: 600,
                    minWidth: 92,
                    textAlign: 'right',
                  }}
                >
                  {value(range, 'max', row)}
                </Typography>
              </Stack>
            </Box>
          );
        })}
      </Stack>
    </Section>
  );
}
