import { Box, CircularProgress, useMediaQuery } from '@mui/material';
import React, { createRef, useEffect, useState } from 'react';
import YearSelection from '@/components/car/bill/YearSelection';
import { useQuery } from '@tanstack/react-query';
import { getAllFuelBillYearsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useFuelBillPagination from '@/hooks/useFuelBillPagination';
import FuelTable from '@/components/car/bill/fuel/FuelTable';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { useTranslation } from 'react-i18next';
import { useScrollNearBottom } from '@/hooks/useScrollNearBottom';
import CarLedgerPage from '@/components/CarLedgerPage';
import useDeleteBillMutation from '@/hooks/useDeleteBillMutation';

interface FuelPageProps {
  id?: string;
  carId: number;
}

export default function FuelPage({ id, carId }: FuelPageProps) {
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
    ...getAllFuelBillYearsOptions({
      client: localClient,
      path: {
        carId,
      },
    }),
  });

  const {
    setYear,
    pagination,
    data: billData,
    setPagination,
    setSort,
    refetch: billRefetch,
  } = useFuelBillPagination(carId);

  const { onDelete } = useDeleteBillMutation(carId, yearRefetch, billRefetch);

  const years: number[] = yearData ?? [];
  const bills = billData?.data ?? [];

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
  });

  async function updateYear(year: number) {
    setSelectedYear(year);
    setYear(year);
  }

  if (isYearLoading || isYearError) {
    return <CircularProgress />;
  }

  const totalBills = billData?.total ?? 0;

  const getHeader = (isMobile: boolean) => (
    <CarLedgerPageHeader
      title={t('app.car.fuel.table.title')}
      isMobile={isMobile}
    />
  );

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <CarLedgerPage id="FuelPage" ref={gridRef}>
        <Box sx={{ p: 2 }} id={id}>
          {getHeader(isMobile)}

          <YearSelection
            years={years}
            selectedYear={selectedYear}
            setSelectedYear={updateYear}
            isMobile
          />

          <FuelTable
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
    <CarLedgerPage id="FuelPage">
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
          <FuelTable
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
