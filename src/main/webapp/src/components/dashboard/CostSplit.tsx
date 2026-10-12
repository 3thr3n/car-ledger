import { Box, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { BillType, TotalStats } from '@/generated';
import { billTypes } from '@/utils/billTypes';
import { num, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';

interface Part {
  type: BillType;
  amount: number;
  count?: number;
}

/**
 * Splits the total into the bill types. Recurring payments have no own field,
 * they are the part of the total not covered by the other types.
 */
export default function CostSplit({ total }: { total: TotalStats }) {
  const { t } = useTranslation();
  const format = useFormat();

  const fuel = num(total.fuelTotal) ?? 0;
  const maintenance = num(total.maintenanceTotal) ?? 0;
  const miscellaneous = num(total.miscellaneousTotal) ?? 0;
  const sum = num(total.total) ?? 0;
  const recurring = Math.max(0, sum - fuel - maintenance - miscellaneous);

  const parts: Part[] = [
    { type: BillType.FUEL, amount: fuel, count: total.fuelBills },
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
  ];

  const share = (amount: number) => (sum > 0 ? (amount / sum) * 100 : 0);

  return (
    <Section
      title={t('dashboard.split.title')}
      subtitle={t('dashboard.split.subtitle')}
    >
      <Box
        role="img"
        aria-label={t('dashboard.split.title')}
        sx={{
          display: 'flex',
          height: 14,
          borderRadius: 999,
          overflow: 'hidden',
          bgcolor: 'action.hover',
          gap: '2px',
          mb: 2.5,
        }}
      >
        {parts
          .filter((part) => part.amount > 0)
          .map((part) => (
            <Box
              key={part.type}
              sx={{
                width: `${share(part.amount)}%`,
                bgcolor: `${billTypes[part.type].color}.main`,
                minWidth: 4,
              }}
            />
          ))}
      </Box>

      <Stack spacing={1.25}>
        {parts.map((part) => (
          <Stack
            key={part.type}
            direction="row"
            spacing={1.5}
            sx={{
              alignItems: 'center',
            }}
          >
            <Box
              sx={{
                width: 10,
                height: 10,
                borderRadius: '3px',
                flexShrink: 0,
                bgcolor: `${billTypes[part.type].color}.main`,
              }}
            />
            <Typography variant="body2" sx={{ flexGrow: 1 }}>
              {t(`bill.types.${part.type}`)}
              {part.count !== undefined && (
                <Typography
                  component="span"
                  variant="body2"
                  sx={{
                    color: 'text.secondary',
                  }}
                >
                  {' · '}
                  {t('dashboard.split.count', { count: part.count })}
                </Typography>
              )}
            </Typography>
            <Typography
              variant="body2"
              className="tabular"
              sx={{
                color: 'text.secondary',
                width: 48,
                textAlign: 'right',
              }}
            >
              {format.number(share(part.amount), 0)} %
            </Typography>
            <Typography
              variant="body2"
              className="tabular"
              sx={{
                fontWeight: 600,
                minWidth: 96,
                textAlign: 'right',
              }}
            >
              {format.money(part.amount)}
            </Typography>
          </Stack>
        ))}
      </Stack>
    </Section>
  );
}
