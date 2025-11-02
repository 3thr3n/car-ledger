import {
  Box,
  Card,
  CircularProgress,
  Grid,
  Typography,
  useMediaQuery,
} from '@mui/material';
import CarBillGrid from '@/components/bill/CarBillGrid';
import { useQuery } from '@tanstack/react-query';
import { getAllBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useState } from 'react';
import { CarPojo } from '@/generated';

interface CarBillProps {
  id: string;
  car: CarPojo | undefined;
}

export default function CarBill({ id, car }: CarBillProps) {
  const isMobile = useMediaQuery('(max-width:600px)');

  const [page, setPage] = useState(0);
  const pageSize = 10; // default page size

  const {
    data: bills,
    isLoading: isBillsLoading,
    isError: isBillsError,
  } = useQuery({
    ...getAllBillsOptions({
      client: localClient,
      path: {
        carId: Number(id),
      },
      query: {
        page,
        size: pageSize,
      },
    }),
    enabled: !!car,
  });

  if (isMobile) {
    if (isBillsLoading) {
      return <CircularProgress />;
    }
    if (isBillsError || !bills) {
      return <Typography color="error">Could not load bills.</Typography>;
    }
    // Mobile: render cards
    return (
      <Box sx={{ p: 2 }}>
        {bills.data?.map((bill) => (
          <Card key={bill.id} sx={{ mb: 2, p: 2 }}>
            <Typography variant="subtitle1" gutterBottom>
              {bill.day}
            </Typography>
            <Grid container spacing={1}>
              <Grid sx={{ xs: 6 }}>
                <Typography>Distance:</Typography>
                <Typography>Unit:</Typography>
                <Typography>Price/Unit:</Typography>
              </Grid>
              <Grid sx={{ xs: 6 }}>
                <Typography>{bill.distance} km</Typography>
                <Typography>{bill.unit}</Typography>
                <Typography>{bill.pricePerUnit} €</Typography>
              </Grid>
              <Grid sx={{ xs: 6 }}>
                <Typography>Estimate:</Typography>
                <Typography>Calculated:</Typography>
                <Typography>Total:</Typography>
              </Grid>
              <Grid sx={{ xs: 6 }}>
                <Typography>{bill.estimate} €</Typography>
                <Typography>{bill.calculated} €</Typography>
                <Typography>{bill.calculatedPrice} €</Typography>
              </Grid>
            </Grid>
          </Card>
        ))}
      </Box>
    );
  }

  return (
    <>
      {isBillsLoading ? (
        <CircularProgress />
      ) : isBillsError || !bills ? (
        <Typography color="error">Could not load bills.</Typography>
      ) : bills.data?.length === 0 ? (
        <Typography>No bills yet</Typography>
      ) : (
        <CarBillGrid
          bills={bills}
          onPageChange={(newPage) => setPage(newPage.page)}
        />
      )}
    </>
  );
}
