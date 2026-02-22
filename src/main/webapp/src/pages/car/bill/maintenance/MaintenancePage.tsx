import { Box, CircularProgress, useMediaQuery } from '@mui/material';
import React, { createRef, useEffect, useState } from 'react';
import YearSelection from '@/components/car/bill/YearSelection';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  deleteBillMutation,
  getAllMaintenanceBillYearsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { useTranslation } from 'react-i18next';
import { useScrollNearBottom } from '@/hooks/useScrollNearBottom';
import CarLedgerPage from '@/components/CarLedgerPage';
import MaintenanceTable from '@/components/car/bill/maintenance/MaintenanceTable';
import useMaintenanceBillPagination from '@/hooks/useMaintenanceBillPagination';

interface MaintenancePageProps {
  id?: string;
  carId: number;
}

export default function MaintenancePage({ id, carId }: MaintenancePageProps) {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width:900px)');

  const gridRef = createRef<HTMLDivElement>();
  const isNearBottom = useScrollNearBottom(gridRef);

  const {
    data: yearData,
    isError: isYearError,
    isLoading: isYearLoading,
    refetch: yearRefetch,
  } = useQuery({
    ...getAllMaintenanceBillYearsOptions({
      client: localClient,
      path: {
        carId,
      },
    }),
  });

  const {
    setYear,
    pagination,
    data: maintenanceBill,
    setPagination,
    setSort,
    refetch: billRefetch,
  } = useMaintenanceBillPagination(carId);

  const { mutate } = useMutation({
    ...deleteBillMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Bill deleted!');
    },
    onSettled: async () => {
      await yearRefetch();
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

  const years: number[] = yearData ?? [];
  const bills = maintenanceBill?.data ?? [];

  const currentYear = new Date().getFullYear();
  const [selectedYear, setSelectedYear] = useState<number>(
    years.includes(currentYear) ? currentYear : -1,
  );

  useEffect(() => {
    if (isNearBottom) {
      setPagination({
        page: pagination.page + 1,
        pageSize: pagination.pageSize,
      });
    }
  }, [isNearBottom, setPagination]);

  async function updateYear(year: number) {
    setSelectedYear(year);
    setYear(year);
  }

  if (isYearLoading || isYearError) {
    return <CircularProgress />;
  }

  const totalBills = maintenanceBill?.total ?? 0;

  const getHeader = (isMobile: boolean) => (
    <CarLedgerPageHeader
      title={t('app.car.maintenance.table.title')}
      isMobile={isMobile}
    />
  );

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <CarLedgerPage id="MaintenancePage" ref={gridRef}>
        <Box sx={{ p: 2 }} id={id}>
          {getHeader(isMobile)}

          <YearSelection
            years={years}
            selectedYear={selectedYear}
            setSelectedYear={updateYear}
            isMobile
          />

          <MaintenanceTable
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
    <CarLedgerPage id="MaintenancePage">
      <Box sx={{ p: 2 }} id={id}>
        {getHeader(isMobile)}
        <Box sx={{ display: 'flex', gap: 3 }}>
          {/* Year Selector */}
          <YearSelection
            years={years}
            selectedYear={selectedYear}
            setSelectedYear={updateYear}
          />

          {/* DataGrid */}
          <MaintenanceTable
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
