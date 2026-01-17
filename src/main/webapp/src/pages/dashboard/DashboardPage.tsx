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
import DashboardDateRange, {
  convertDateRangeToQuery,
} from '@/components/dashboard/DashboardDateRange';
import { NavigateOptions } from '@tanstack/router-core';
import { useSyncQueryParams } from '@/hooks/useSyncQueryParams';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation();

  const [selectedCarId, setSelectedCarId] = useState<number>(
    isNaN(Number(search.selectedCar)) ? -1 : Number(search.selectedCar),
  );
  const [dateRange, setDateRange] = useState<DashboardDateRange>({
    to: search.to ? dayjs(search.to) : undefined,
    from: search.from ? dayjs(search.from) : undefined,
  });

  useSyncQueryParams(navigate, search, selectedCarId, dateRange);

  const { data: total, isLoading: isTotalLoading } = useQuery({
    ...getStatsTotalOptions({
      client: localClient,
      path: { carId: selectedCarId },
      query: convertDateRangeToQuery(dateRange),
    }),
    enabled: selectedCarId !== null,
  });

  const { data: average, isLoading: isAverageLoading } = useQuery({
    ...getStatsAverageOptions({
      client: localClient,
      path: { carId: selectedCarId },
      query: convertDateRangeToQuery(dateRange),
    }),
    enabled: selectedCarId !== null,
  });

  return (
    <Container sx={{ py: 4 }}>
      <CarLedgerPageHeader
        title={t('app.dashboard.title')}
        navigate={navigate}
      />

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
