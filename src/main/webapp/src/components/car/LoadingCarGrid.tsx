import { Skeleton } from '@mui/material';
import { CarGrid } from './CarGrid';

export function LoadingCarGrid() {
  return (
    <CarGrid>
      <Skeleton variant="rectangular" height={100} width="100%" />
      <Skeleton variant="text" width="100%" sx={{ fontSize: '2rem' }} />
    </CarGrid>
  );
}
