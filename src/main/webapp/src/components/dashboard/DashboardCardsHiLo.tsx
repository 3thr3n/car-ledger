import { Grid, Typography } from '@mui/material';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';
import { HiLoStats } from '@/generated';
import DashboardMetricHiLo from '@/components/dashboard/DashboardMetricHiLo';

export interface DashboardCardsHiLoProps {
  width: number | string;
  hiLo: HiLoStats;
}

export default function DashboardCardsHiLo({
  width,
  hiLo,
}: DashboardCardsHiLoProps) {
  return (
    <>
      <Typography
        variant="h6"
        color="gray"
        sx={{ gridColumn: '1 / -1', mt: 2 }}
      >
        High / Low
      </Typography>
      <Grid container spacing={3} justifyContent="center" sx={{ mt: 3 }}>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={4}>
            <DashboardMetricHiLo
              label="Price per unit"
              invert
              high={
                hiLo.pricePerUnit
                  ? Number(hiLo.pricePerUnit.max).toFixed(2)
                  : '-'
              }
              low={
                hiLo.pricePerUnit
                  ? Number(hiLo.pricePerUnit.min).toFixed(2)
                  : '-'
              }
              unit="ct"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 8, md: 4 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={0}>
            <DashboardMetricHiLo
              label="Tracked distance"
              high={hiLo.distance ? Number(hiLo.distance.max).toFixed(2) : '-'}
              low={hiLo.distance ? Number(hiLo.distance.min).toFixed(2) : '-'}
              unit="km"
              type="distance"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 8, md: 4 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={3}>
            <DashboardMetricHiLo
              label="Consumption"
              invert
              high={
                hiLo.consumption ? Number(hiLo.consumption.max).toFixed(2) : '-'
              }
              low={
                hiLo.consumption ? Number(hiLo.consumption.min).toFixed(2) : '-'
              }
              unit="l/100km"
              type="fuel"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetricHiLo
              label="Fuel cost per KM"
              invert
              high={
                hiLo.fuelCostPerKm
                  ? Number(hiLo.fuelCostPerKm.max).toFixed(2)
                  : '-'
              }
              low={
                hiLo.fuelCostPerKm
                  ? Number(hiLo.fuelCostPerKm.min).toFixed(2)
                  : '-'
              }
              unit="ct"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={1}>
            <DashboardMetricHiLo
              label="Fuel cost"
              invert
              high={hiLo.fuelCost ? Number(hiLo.fuelCost.max).toFixed(2) : '-'}
              low={hiLo.fuelCost ? Number(hiLo.fuelCost.min).toFixed(2) : '-'}
              unit="€"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
      </Grid>
    </>
  );
}
