import { Grid, Typography } from '@mui/material';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';
import DashboardMetric from '@/components/dashboard/DashboardMetric';
import { AverageStats } from '@/generated';

export interface DashboardCardsAverageProps {
  width: number | string;
  average: AverageStats;
}

export default function DashboardCardsAverage({
  width,
  average,
}: DashboardCardsAverageProps) {
  return (
    <>
      <Typography
        variant="h6"
        color="gray"
        sx={{ gridColumn: '1 / -1', mt: 2 }}
      >
        Average
      </Typography>
      <Grid container spacing={3} justifyContent="center" sx={{ mt: 3 }}>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={1}>
            <DashboardMetric
              label="Price per unit"
              value={
                average.pricePerUnit
                  ? Number(average.pricePerUnit).toFixed(2)
                  : '-'
              }
              unit="ct"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetric
              label="Distance"
              value={
                average.distance ? Number(average.distance).toFixed(2) : '-'
              }
              unit="km"
              type="distance"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={0}>
            <DashboardMetric
              label="Consumption"
              value={
                average.fuelConsumption
                  ? Number(average.fuelConsumption).toFixed(2)
                  : '-'
              }
              unit="l/100km"
              type="fuel"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetric
              label="Cost per KM"
              value={
                average.costPerKm ? Number(average.costPerKm).toFixed(2) : '-'
              }
              unit="ct"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetric
              label="Fuel cost per KM"
              value={
                average.fuelCostPerKm
                  ? Number(average.fuelCostPerKm).toFixed(2)
                  : '-'
              }
              unit="ct"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetric
              label="Maintenance cost"
              value={
                average.maintenanceCost
                  ? Number(average.maintenanceCost).toFixed(2)
                  : '-'
              }
              unit="€"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
      </Grid>
    </>
  );
}
