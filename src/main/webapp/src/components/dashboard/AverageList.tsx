import { Divider, Stack, Typography } from '@mui/material';
import { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { AverageStats } from '@/generated';
import { useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';

export default function AverageList({ average }: { average: AverageStats }) {
  const { t } = useTranslation();
  const format = useFormat();

  const rows = [
    {
      key: 'pricePerUnit',
      value: format.unit(average.pricePerUnit, 'ct/l', 1),
    },
    { key: 'distance', value: format.unit(average.distance, 'km', 0) },
    {
      key: 'fuelCostPerKm',
      value: format.unit(average.fuelCostPerKm, 'ct/km'),
    },
    { key: 'maintenanceCost', value: format.money(average.maintenanceCost) },
  ];

  return (
    <Section title={t('dashboard.average.title')}>
      <Stack>
        {rows.map((row, index) => (
          <Fragment key={row.key}>
            {index > 0 && <Divider />}
            <Stack
              direction="row"
              justifyContent="space-between"
              sx={{ py: 1.25 }}
            >
              <Typography variant="body2" color="text.secondary">
                {t(`dashboard.average.${row.key}`)}
              </Typography>
              <Typography variant="body2" fontWeight={600} className="tabular">
                {row.value}
              </Typography>
            </Stack>
          </Fragment>
        ))}
      </Stack>
    </Section>
  );
}
