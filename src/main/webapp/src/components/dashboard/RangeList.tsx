import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
} from '@mui/material';
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

  const cell = {
    py: 1.25,
    px: 0,
    borderColor: 'divider',
  } as const;
  const figure = { ...cell, fontWeight: 600, whiteSpace: 'nowrap' } as const;

  return (
    <Section
      title={t('dashboard.range.title')}
      subtitle={t('dashboard.range.subtitle')}
    >
      <Table
        size="small"
        sx={{ '& tr:last-of-type td, & tr:last-of-type th': { border: 0 } }}
      >
        <TableHead>
          <TableRow>
            <TableCell sx={{ ...cell, color: 'text.secondary' }}>
              {t('dashboard.range.metric')}
            </TableCell>
            <TableCell align="right" sx={{ ...cell, color: 'text.secondary' }}>
              {t('dashboard.range.lowest')}
            </TableCell>
            <TableCell
              align="right"
              sx={{ ...cell, color: 'text.secondary', pl: 2 }}
            >
              {t('dashboard.range.highest')}
            </TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {shown.map((row) => (
            <TableRow key={row.key}>
              <TableCell component="th" scope="row" sx={cell}>
                {t(`dashboard.range.${row.key}`, {
                  context: format.units.distance.context,
                })}
              </TableCell>
              <TableCell align="right" className="tabular" sx={figure}>
                {value(hiLo[row.key], 'min', row)}
              </TableCell>
              <TableCell
                align="right"
                className="tabular"
                sx={{ ...figure, pl: 2 }}
              >
                {value(hiLo[row.key], 'max', row)}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </Section>
  );
}
