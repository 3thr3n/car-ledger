import { Divider, Stack, Typography } from '@mui/material';
import { Fragment } from 'react';
import { useTranslation } from 'react-i18next';
import { AverageStats } from '@/generated';
import { hasValue, useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';

export default function AverageList({ average }: { average: AverageStats }) {
  const { t } = useTranslation();
  const format = useFormat();

  const charged = hasValue(average.chargingPricePerUnit);
  // without any charges the fuel rows stay, also when they are still empty
  const refueled = hasValue(average.pricePerUnit) || !charged;
  const rows = [
    ...(refueled
      ? [
          {
            key: 'pricePerUnit',
            value: format.fuelPrice(average.pricePerUnit),
          },
          { key: 'distance', value: format.distance(average.distance) },
          {
            key: 'fuelCostPerKm',
            value: format.costPerDistance(average.fuelCostPerKm),
          },
        ]
      : []),
    ...(charged
      ? [
          {
            key: 'chargingPricePerUnit',
            value: format.unit(average.chargingPricePerUnit, 'ct/kWh', 1),
          },
          {
            key: 'chargingCostPerKm',
            value: format.costPerDistance(average.chargingCostPerKm),
          },
        ]
      : []),
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
              sx={{
                justifyContent: 'space-between',
                py: 1.25,
              }}
            >
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                }}
              >
                {t(`dashboard.average.${row.key}`, {
                  context: format.units.distance.context,
                })}
              </Typography>
              <Typography
                variant="body2"
                className="tabular"
                sx={{
                  fontWeight: 600,
                }}
              >
                {row.value}
              </Typography>
            </Stack>
          </Fragment>
        ))}
      </Stack>
    </Section>
  );
}
