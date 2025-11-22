import { Grid, useMediaQuery, useTheme } from '@mui/material';
import DashboardMetric from '@/components/dashboard/DashboardMetric';
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation';
import { AverageStats, MinimalStats, TotalStats } from '@/generated';
import SpeedIcon from '@mui/icons-material/Speed';
import EuroIcon from '@mui/icons-material/Euro';
import RouteIcon from '@mui/icons-material/Route';
import { AnimatedCard } from '@/components/base/AnimatedCard';

export default function DashboardCards({
  minimal,
  average,
  total,
}: {
  minimal?: MinimalStats;
  average?: AverageStats;
  total?: TotalStats;
}) {
  const theme = useTheme();
  const isMd = useMediaQuery(theme.breakpoints.up('md'));
  const isSm = useMediaQuery(theme.breakpoints.up('sm'));

  if (!minimal || !average || !total) return null;

  const width = isMd
    ? `calc(100% / 12 * 3.75)` // md → 3 columns
    : isSm
      ? `calc(100% / 12 * 5.70)` // sm → 6 columns
      : '100%'; // xs → full width

  return (
    <Grid container spacing={3} justifyContent="center" sx={{ mt: 3 }}>
      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <AnimatedCard index={0}>
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
        </AnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <AnimatedCard index={1}>
          <DashboardMetric
            label="Avg Consumption"
            value={
              average?.calculated ? Number(average.calculated).toFixed(1) : '-'
            }
            unit="l/100km"
            color="#ffb74d"
            icon={<SpeedIcon />}
          />
        </AnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <AnimatedCard index={2}>
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
        </AnimatedCard>
      </Grid>

      <Grid columns={{ xs: 12, sm: 6, md: 3 }} sx={{ width }}>
        <AnimatedCard index={3}>
          <DashboardMetric
            label="Avg Distance"
            value={
              average?.distance ? Number(average.distance).toFixed(0) : '-'
            }
            unit="km"
            color="#4dd0e1"
            icon={<RouteIcon />}
          />
        </AnimatedCard>
      </Grid>
    </Grid>
  );
}
