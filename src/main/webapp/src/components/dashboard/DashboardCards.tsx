import { Grid, useMediaQuery, useTheme } from '@mui/material';
import DashboardMetric from '@/components/dashboard/DashboardMetric';
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation';
import { AverageStats, TotalStats } from '@/generated';
import SpeedIcon from '@mui/icons-material/Speed';
import EuroIcon from '@mui/icons-material/Euro';
import RouteIcon from '@mui/icons-material/Route';
import { CarLedgerAnimatedCard } from '@/components/CarLedgerAnimatedCard';

export default function DashboardCards({
  average,
  total,
}: {
  average?: AverageStats;
  total?: TotalStats;
}) {
  const theme = useTheme();
  const isMd = useMediaQuery(theme.breakpoints.up('md'));
  const isSm = useMediaQuery(theme.breakpoints.up('sm'));

  if (!average || !total) return null;

  const width = isMd
    ? `calc(100% / 12 * 3.75)` // md → 3 columns
    : isSm
      ? `calc(100% / 12 * 5.70)` // sm → 6 columns
      : '100%'; // xs → full width

  return (
    <Grid container spacing={3} justifyContent="center" sx={{ mt: 3 }}>
      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <CarLedgerAnimatedCard index={0}>
          <DashboardMetric
            label="Total Spent"
            value={
              total?.calculatedPrice
                ? Number(total.calculatedPrice).toFixed(2)
                : '-'
            }
            unit="€"
            color="#4bc0c0"
            icon={<EuroIcon />}
          />
        </CarLedgerAnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <CarLedgerAnimatedCard index={1}>
          <DashboardMetric
            label="Avg Consumption"
            value={
              average?.calculated ? Number(average.calculated).toFixed(2) : '-'
            }
            unit="l/100km"
            color="#ffb74d"
            icon={<SpeedIcon />}
          />
        </CarLedgerAnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <CarLedgerAnimatedCard index={2}>
          <DashboardMetric
            label="Avg Price per L"
            value={
              average?.calculatedPrice
                ? Number(average.pricePerUnit).toFixed(2)
                : '-'
            }
            unit="ct"
            color="#ba68c8"
            icon={<LocalGasStationIcon />}
          />
        </CarLedgerAnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <CarLedgerAnimatedCard index={3}>
          <DashboardMetric
            label="Avg Distance"
            value={
              average?.distance ? Number(average.distance).toFixed(2) : '-'
            }
            unit="km"
            color="#4dd0e1"
            icon={<RouteIcon />}
          />
        </CarLedgerAnimatedCard>
      </Grid>
    </Grid>
  );
}
