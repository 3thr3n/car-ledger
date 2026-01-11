import { Box, CircularProgress, useMediaQuery } from '@mui/material';
import React, { useState } from 'react';
import { FuelBill } from '@/generated';
import YearSelection from '@/components/car/fuel/YearSelection';
import { useMutation, useQuery } from '@tanstack/react-query';
import {
  deleteBillMutation,
  getAllBillYearsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useBillPagination from '@/hooks/useBillPagination';
import FuelTable from '@/components/car/fuel/FuelTable';
import { toast } from 'react-toastify';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { NavigateOptions } from '@tanstack/router-core';

interface CarBillOverviewProps {
  id: number;
  navigate: (path: NavigateOptions) => void;
}

export default function AllViewPage({ id, navigate }: CarBillOverviewProps) {
  const isMobile = useMediaQuery('(max-width:900px)');

  const {
    data: yearData,
    isError: isYearError,
    isLoading: isYearLoading,
    refetch: yearRefetch,
  } = useQuery({
    ...getAllBillYearsOptions({
      client: localClient,
      path: {
        carId: id,
      },
    }),
  });

  const {
    setYear,
    data: billData,
    setPagination,
    setSort,
    refetch: billRefetch,
  } = useBillPagination(id);

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
        carId: id,
        billId,
      },
    });
  }

  const years: number[] = yearData ?? [];
  const bills: FuelBill[] = billData?.data ?? [];

  const currentYear = new Date().getFullYear();
  const [selectedYear, setSelectedYear] = useState<number>(
    years.includes(currentYear) ? currentYear : -1,
  );

  async function updateYear(year: number) {
    setSelectedYear(year);
    setYear(year);
  }

  if (isYearLoading || isYearError) {
    return <CircularProgress />;
  }

  const totalBills = billData?.total ?? 0;
  const navigateTo: NavigateOptions = {
    to: '/car/$id',
    params: { id: `${id}` },
  };

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <Box sx={{ p: 2 }}>
        <CarLedgerPageHeader
          title="Fuel Entries"
          navigate={navigate}
          navigateTo={navigateTo}
          isMobile
        />

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
    );
  }

  // 💻 Desktop view: sidebar selector + table
  return (
    <Box sx={{ p: 2 }}>
      <CarLedgerPageHeader
        title="Fuel Entries"
        navigate={navigate}
        navigateTo={navigateTo}
      />

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
  );
}
