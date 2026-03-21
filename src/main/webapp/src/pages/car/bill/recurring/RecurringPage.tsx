import { Box, useMediaQuery } from '@mui/material';
import React, { createRef, useEffect } from 'react';
import { useMutation } from '@tanstack/react-query';
import { deleteRecurringBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { useTranslation } from 'react-i18next';
import { useScrollNearBottom } from '@/hooks/useScrollNearBottom';
import CarLedgerPage from '@/components/CarLedgerPage';
import RecurringTable from '@/components/car/bill/recurring/RecurringTable';
import useRecurringBillPagination from '@/hooks/useRecurringBillPagination';

interface RecurringPageProps {
  id?: string;
  carId: number;
}

export default function RecurringPage({ id, carId }: RecurringPageProps) {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width:900px)');

  const gridRef = createRef<HTMLDivElement>();
  const isNearBottom = useScrollNearBottom(gridRef);

  const {
    pagination,
    data: pagedBills,
    setPagination,
    setSort,
    refetch: billRefetch,
  } = useRecurringBillPagination(carId);

  const { mutate } = useMutation({
    ...deleteRecurringBillMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Bill deleted!');
    },
    onSettled: async () => {
      await billRefetch();
    },
  });

  function onDelete(billId: number) {
    mutate({
      path: {
        carId,
        billId,
      },
    });
  }

  const bills = pagedBills?.data ?? [];

  useEffect(() => {
    if (isNearBottom) {
      setPagination({
        page: pagination.page + 1,
        pageSize: pagination.pageSize,
      });
    }
  });

  const totalBills = pagedBills?.total ?? 0;

  const getHeader = (isMobile: boolean) => (
    <CarLedgerPageHeader
      title={t('app.car.recurring.table.title')}
      isMobile={isMobile}
    />
  );

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <CarLedgerPage id="RecurringPage" ref={gridRef}>
        <Box sx={{ p: 2 }} id={id}>
          {getHeader(isMobile)}

          <RecurringTable
            onDelete={onDelete}
            setPagination={setPagination}
            setSortModel={setSort}
            totalBills={totalBills}
            bills={bills}
            isMobile
          />
        </Box>
      </CarLedgerPage>
    );
  }

  // 💻 Desktop view: sidebar selector + table

  return (
    <CarLedgerPage id="RecurringPage">
      <Box sx={{ p: 2 }} id={id}>
        {getHeader(isMobile)}
        <Box sx={{ display: 'flex', gap: 3 }}>
          {/* DataGrid */}
          <RecurringTable
            onDelete={onDelete}
            bills={bills}
            setPagination={setPagination}
            totalBills={totalBills}
            setSortModel={setSort}
          />
        </Box>
      </Box>
    </CarLedgerPage>
  );
}
