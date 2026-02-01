import {
  Box,
  CircularProgress,
  Container,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import React, { useState } from 'react';
import { getDashboardStatsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import DashboardFilterBar from '@/components/dashboard/DashboardFilterBar';
import DashboardDateRange, {
  convertDateRangeToQuery,
} from '@/components/dashboard/DashboardDateRange';
import { NavigateOptions } from '@tanstack/router-core';
import { useSyncQueryParams } from '@/hooks/useSyncQueryParams';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import DashboardCardsAverage from '@/components/dashboard/DashboardCardsAverage';
import DashboardCardsHiLo from '@/components/dashboard/DashboardCardsHiLo';
import DashboardCardsTotal from '@/components/dashboard/DashboardCardsTotal';
import CarLedgerPage from '@/components/CarLedgerPage';

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
  const theme = useTheme();
  const isMd = useMediaQuery(theme.breakpoints.up('md'));
  const isSm = useMediaQuery(theme.breakpoints.up('sm'));

  const [selectedCarId, setSelectedCarId] = useState<number>(
    isNaN(Number(search.selectedCar)) ? -1 : Number(search.selectedCar),
  );
  const [dateRange, setDateRange] = useState<DashboardDateRange>({
    to: search.to ? dayjs(search.to) : undefined,
    from: search.from ? dayjs(search.from) : undefined,
  });

  useSyncQueryParams(navigate, search, selectedCarId, dateRange);

  const { data, isLoading } = useQuery({
    ...getDashboardStatsOptions({
      client: localClient,
      path: { carId: selectedCarId },
      query: convertDateRangeToQuery(dateRange),
    }),
    enabled: selectedCarId !== null,
  });

  const width = isMd
    ? `calc(100% / 12 * 3.75)` // md → 3 columns
    : isSm
      ? `calc(100% / 12 * 5.70)` // sm → 6 columns
      : '100%'; // xs → full width

  return (
    <CarLedgerPage id="DashboardPage">
      <Container>
        <CarLedgerPageHeader title={t('app.dashboard.title')} />

        <DashboardFilterBar
          selectedCarId={selectedCarId}
          onSelectCar={setSelectedCarId}
          dateRange={dateRange}
          onChangeDateRange={setDateRange}
        />

        {isLoading || !data ? (
          <CircularProgress />
        ) : (
          <Box sx={{ mt: 3 }}>
            <DashboardCardsTotal width={width} total={data.total!} />
            <DashboardCardsAverage width={width} average={data.average!} />
            <DashboardCardsHiLo width={width} hiLo={data.hiLo!} />
          </Box>
        )}
      </Container>
    </CarLedgerPage>
  );
}
