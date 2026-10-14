import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { HiLo, HiLoStats } from '@/generated';
import { EMPTY, hasValue, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';

interface Row {
  key: keyof HiLoStats;
  unit: string;
  digits: number;
  money?: boolean;
  /** only shown with refuels or with charges */
  energy?: 'fuel' | 'charging';
}

const rows: Row[] = [
  { key: 'pricePerUnit', unit: 'ct/l', digits: 1, energy: 'fuel' },
  { key: 'consumption', unit: 'l/100km', digits: 2, energy: 'fuel' },
  { key: 'fuelCostPerKm', unit: 'ct/km', digits: 2, energy: 'fuel' },
  { key: 'distance', unit: 'km', digits: 0, energy: 'fuel' },
  { key: 'fuelCost', unit: '€', digits: 2, money: true, energy: 'fuel' },
  {
    key: 'chargingPricePerUnit',
    unit: 'ct/kWh',
    digits: 1,
    energy: 'charging',
  },
  {
    key: 'chargingConsumption',
    unit: 'kWh/100km',
    digits: 2,
    energy: 'charging',
  },
  {
    key: 'chargingCost',
    unit: '€',
    digits: 2,
    money: true,
    energy: 'charging',
  },
  { key: 'maintenanceCost', unit: '€', digits: 2, money: true },
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
    const number = range?.[side];
    if (number === undefined || number === null) return EMPTY;
    return row.money
      ? format.money(number)
      : format.unit(number, row.unit, row.digits);
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
                {t(`dashboard.range.${key}`)}
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
