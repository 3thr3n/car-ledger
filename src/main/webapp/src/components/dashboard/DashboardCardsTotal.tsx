import { Grid, Typography } from '@mui/material';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';
import DashboardMetric from '@/components/dashboard/DashboardMetric';
import { TotalStats } from '@/generated';

export interface DashboardCardsTotalProps {
  width: number | string;
  total: TotalStats;
}

export default function DashboardCardsTotal({
  width,
  total,
}: DashboardCardsTotalProps) {
  return (
    <>
      <Typography
        variant="h6"
        color="gray"
        sx={{ gridColumn: '1 / -1', mt: 2 }}
      >
        Total
      </Typography>
      <Grid container spacing={3} justifyContent="center" sx={{ mt: 3 }}>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={0}>
            <DashboardMetric
              label="Spent"
              value={total.total ? Number(total.total).toFixed(2) : '-'}
              unit="€"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>

        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={1}>
            <DashboardMetric
              label="Tracked distance"
              value={
                total.trackedDistance
                  ? Number(total.trackedDistance).toFixed(2)
                  : '-'
              }
              unit="km"
              type="distance"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={2}>
            <DashboardMetric
              label="Unit"
              value={total.unit ? Number(total.unit).toFixed(2) : '-'}
              unit="L"
              type="fuel"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={3}>
            <DashboardMetric
              label="Fuel cost"
              value={total.fuelTotal ? Number(total.fuelTotal).toFixed(2) : '-'}
              unit="€"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={3}>
            <DashboardMetric
              label="Maintenance cost"
              value={
                total.maintenanceTotal
                  ? Number(total.maintenanceTotal).toFixed(2)
                  : '-'
              }
              unit="€"
              type="cost"
            />
          </CarLedgerAnimatedCard>
        </Grid>
        <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
          <CarLedgerAnimatedCard index={3}>
            <DashboardMetric
              label="Miscellaneous cost"
              value={
                total.miscellaneousTotal
                  ? Number(total.miscellaneousTotal).toFixed(2)
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
