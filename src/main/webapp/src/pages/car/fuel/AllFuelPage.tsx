import { Box, CircularProgress, useMediaQuery } from '@mui/material';
import React, { useState } from 'react';
import { BillPojo } from '@/generated';
import YearSelection from '@/components/car/fuel/YearSelection';
import { useQuery } from '@tanstack/react-query';
import { getAllBillYearsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useBillPagination from '@/hooks/useBillPagination';
import FuelTable from '@/components/car/fuel/FuelTable';
import PageHeader from '@/components/base/PageHeader';

interface CarBillOverviewProps {
  id: number;
  onDelete: (id: number) => void;
}

export default function AllViewPage({ id, onDelete }: CarBillOverviewProps) {
  const isMobile = useMediaQuery('(max-width:900px)');

  const {
    data: yearData,
    isError: isYearError,
    isLoading: isYearLoading,
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
  } = useBillPagination(id);

  const years: number[] = yearData ?? [];
  const bills: BillPojo[] = billData?.data ?? [];

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

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <Box sx={{ p: 2 }}>
        <PageHeader title="Fuel Entries" isMobile />

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
      <PageHeader title="Fuel Entries" />

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
