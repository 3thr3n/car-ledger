import {
  Box,
  Button,
  CircularProgress,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getAllBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useEffect } from 'react';

export default function CarBillPreviewTable({
  id,
  onSeeMore,
  reload,
}: {
  id: string;
  onSeeMore: () => void;
  reload: number;
}) {
  const {
    data: bills,
    isLoading: isBillsLoading,
    isError: isBillsError,
    refetch: billRefetch,
  } = useQuery({
    ...getAllBillsOptions({
      client: localClient,
      path: {
        carId: Number(id),
      },
      query: {
        page: 1,
        size: 7,
      },
    }),
  });

  useEffect(() => {
    if (reload != 0) {
      billRefetch();
    }
  }, [billRefetch, reload]);

  if (isBillsLoading) {
    return (
      <Box>
        <CircularProgress />
        <Typography>Loading bills...</Typography>
      </Box>
    );
  }

  if (isBillsError) {
    return (
      <Box>
        <Typography color="error">Loading bills failed!</Typography>
      </Box>
    );
  }

  const latestBills = bills?.data ?? [];

  return (
    <Box>
      <TableContainer sx={{ maxHeight: 360 }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow>
              <TableCell>Date</TableCell>
              <TableCell align="right">Distance</TableCell>
              <TableCell align="right">Unit (L)</TableCell>
              <TableCell align="right">Price/Unit</TableCell>
              <TableCell align="right">Total</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {latestBills.map((bill) => (
              <TableRow key={bill.id}>
                <TableCell>{bill.day}</TableCell>
                <TableCell align="right">{bill.distance}</TableCell>
                <TableCell align="right">{bill.unit}</TableCell>
                <TableCell align="right">{bill.pricePerUnit} €</TableCell>
                <TableCell align="right">{bill.calculatedPrice} €</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box display="flex" justifyContent="flex-end" mt={2}>
        <Button size="small" onClick={onSeeMore}>
          See all entries →
        </Button>
      </Box>
    </Box>
  );
}
