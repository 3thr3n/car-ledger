import { Box, Divider, Stack, Typography } from '@mui/material';
import { Fragment, ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { Car } from '@/generated';
import { useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import { RouterButton } from '@/components/ui/links';

/**
 * Plate, VIN, tank and battery and purchase of the car; with a purchase price also the cost of
 * ownership, the purchase plus all costs of the ledger.
 *
 * @param totalCost costs of all entries and recurring payments of the car
 */
export default function CarDetails({
  car,
  totalCost,
}: {
  car: Car;
  totalCost?: number;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  const params = { id: `${car.id}` };

  const rows: { key: string; value: ReactNode }[] = [];
  if (car.licensePlate) {
    rows.push({ key: 'licensePlate', value: car.licensePlate });
  }
  if (car.vin) {
    rows.push({
      key: 'vin',
      value: (
        <Box component="span" sx={{ fontFamily: 'monospace' }}>
          {car.vin}
        </Box>
      ),
    });
  }
  if (car.tankSize) {
    rows.push({ key: 'tankSize', value: format.unit(car.tankSize, 'l', 1) });
  }
  if (car.batteryCapacity) {
    rows.push({
      key: 'batteryCapacity',
      value: format.unit(car.batteryCapacity, 'kWh', 1),
    });
  }
  if (car.purchaseDate) {
    rows.push({ key: 'purchaseDate', value: format.date(car.purchaseDate) });
  }
  if (car.purchasePrice !== undefined && car.purchasePrice !== null) {
    rows.push({
      key: 'purchasePrice',
      value: format.money(car.purchasePrice),
    });
    rows.push({
      key: 'ownership',
      value: (
        <Box component="span" sx={{ fontWeight: 650 }}>
          {format.money(car.purchasePrice + (totalCost ?? 0))}
        </Box>
      ),
    });
  }

  const edit = (
    <RouterButton to="/car/$id/edit" params={params} size="small">
      {t('button.edit')}
    </RouterButton>
  );

  return (
    <Section title={t('car.details.title')} action={rows.length ? edit : null}>
      {rows.length ? (
        <Stack>
          {rows.map((row, index) => (
            <Fragment key={row.key}>
              {index > 0 && <Divider />}
              <Stack
                direction="row"
                spacing={2}
                sx={{
                  justifyContent: 'space-between',
                  alignItems: 'baseline',
                  py: 1.25,
                }}
              >
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t(`car.details.${row.key}`)}
                </Typography>
                <Typography
                  className="tabular"
                  sx={{ textAlign: 'right', overflowWrap: 'anywhere' }}
                >
                  {row.value}
                </Typography>
              </Stack>
            </Fragment>
          ))}
        </Stack>
      ) : (
        <EmptyState
          compact
          title={t('car.details.empty')}
          action={
            <RouterButton
              to="/car/$id/edit"
              params={params}
              size="small"
              variant="outlined"
            >
              {t('car.details.add')}
            </RouterButton>
          }
        />
      )}
    </Section>
  );
}
