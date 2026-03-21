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
import { getAllRecurringBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

export default function RecurringPreviewTable({
  id,
  onSeeMore,
  reload,
}: {
  id: string;
  onSeeMore: () => void;
  reload: number;
}) {
  const { t } = useTranslation();

  const {
    data: bills,
    isLoading: isBillsLoading,
    isError: isBillsError,
    refetch: billRefetch,
  } = useQuery({
    ...getAllRecurringBillsOptions({
      client: localClient,
      path: {
        carId: Number(id),
      },
      query: {
        page: 1,
        size: 7,
        onlyRunning: true,
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
        <Typography>{t('app.car.fuel.table.loading')}...</Typography>
      </Box>
    );
  }

  if (isBillsError) {
    return (
      <Box>
        <Typography color="error">
          {t('app.car.fuel.table.loadingFailed')}
        </Typography>
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
              <TableCell>{t('app.car.common.name')}</TableCell>
              <TableCell align="right">
                {t('app.car.common.nextDueDate')}
              </TableCell>
              <TableCell align="right">{t('app.car.common.endDate')}</TableCell>
              <TableCell align="right">{t('app.car.common.amount')}</TableCell>
              <TableCell align="right">{t('app.car.common.total')}</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {latestBills.map((bill, i) => (
              <TableRow key={bill.id} className={i % 2 === 0 ? 'even' : 'odd'}>
                <TableCell>{bill.name}</TableCell>
                <TableCell align="right">{bill.nextDueDate}</TableCell>
                <TableCell align="right">{bill.endDate}</TableCell>
                <TableCell align="right">
                  {Number(bill.amount).toFixed(2)} €
                </TableCell>
                <TableCell align="right">
                  {Number(bill.total).toFixed(2)} €
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box display="flex" justifyContent="flex-end" mt={2}>
        <Button size="small" onClick={onSeeMore}>
          {t('app.car.common.seeMore')} →
        </Button>
      </Box>
    </Box>
  );
}
