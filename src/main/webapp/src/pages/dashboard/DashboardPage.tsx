import { Box, CircularProgress, Container } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import React, { useState } from 'react';
import {
  getStatsAverageOptions,
  getStatsTotalOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import DashboardFilterBar from '@/components/dashboard/DashboardFilterBar';
import DashboardCards from '@/components/dashboard/DashboardCards';
import DashboardDateRange from '@/components/dashboard/DashboardDateRange';
import { NavigateOptions } from '@tanstack/router-core';
import { useSyncQueryParams } from '@/hooks/useSyncQueryParams';
import PageHeader from '@/components/base/PageHeader';

// import HiLoSummary from '@/components/dashboard/HiLoSummary';

export interface DashboardPageProps {
  navigate: (nav: NavigateOptions) => void;
  search: DashboardPageSearch;
}

export interface DashboardPageSearch {
  from?: string;
  to?: string;
  selectedCar?: string | number;
}

export default function DashboardPage({
  navigate,
  search,
}: DashboardPageProps) {
  const [selectedCarId, setSelectedCarId] = useState<number>(
    isNaN(Number(search.selectedCar)) ? -1 : Number(search.selectedCar),
  );
  const [dateRange, setDateRange] = useState<DashboardDateRange>({
    to: search.to,
    from: search.from,
  });

  useSyncQueryParams(navigate, search, selectedCarId, dateRange);

  const { data: total, isLoading: isTotalLoading } = useQuery({
    ...getStatsTotalOptions({
      client: localClient,
      path: { carId: selectedCarId },
      query: dateRange,
    }),
    enabled: selectedCarId !== null,
  });

  const { data: average, isLoading: isAverageLoading } = useQuery({
    ...getStatsAverageOptions({
      client: localClient,
      path: { carId: selectedCarId },
      query: dateRange,
    }),
    enabled: selectedCarId !== null,
  });

  return (
    <Container sx={{ py: 4 }}>
      <PageHeader title="Dashboard" navigate={navigate} />

      <DashboardFilterBar
        selectedCarId={selectedCarId}
        onSelectCar={setSelectedCarId}
        dateRange={dateRange}
        onChangeDateRange={setDateRange}
      />

      {isAverageLoading || isTotalLoading ? (
        <CircularProgress />
      ) : (
        <Box sx={{ mt: 3 }}>
          <DashboardCards average={average} total={total} />
        </Box>
      )}
    </Container>
  );
}
