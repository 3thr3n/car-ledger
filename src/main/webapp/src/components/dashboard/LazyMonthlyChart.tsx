import { Skeleton } from '@mui/material';
import { lazy, Suspense } from 'react';
import type { MonthlyChartProps } from './MonthlyChart';

// the chart library is loaded with the first chart, not with the app
const MonthlyChart = lazy(() => import('./MonthlyChart'));

export default function LazyMonthlyChart(props: MonthlyChartProps) {
  return (
    <Suspense
      fallback={
        <Skeleton variant="rounded" height={380} sx={{ borderRadius: 3.5 }} />
      }
    >
      <MonthlyChart {...props} />
    </Suspense>
  );
}
