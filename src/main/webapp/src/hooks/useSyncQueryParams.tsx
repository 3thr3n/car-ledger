import { useEffect } from 'react';
import DashboardDateRange from '@/components/dashboard/DashboardDateRange';
import { NavigateOptions } from '@tanstack/router-core';
import { DashboardPageSearch } from '@/pages/dashboard/DashboardPage';
import { formatDayjs } from '@/utils/DateUtils';

/**
 * Loads and updates on demand the searchQueries in the URL
 *
 * @param navigate
 * @param search SearchQuery of current page
 * @param selectedCarId Current selected car
 * @param dateRange
 */
export function useSyncQueryParams(
  navigate: (nav: NavigateOptions) => void,
  search: DashboardPageSearch,
  selectedCarId: number,
  dateRange: DashboardDateRange,
) {
  useEffect(() => {
    const newParams: DashboardPageSearch = {
      selectedCar: search.selectedCar,
      from: search.from,
      to: search.to,
    };

    // Update carId param
    if (selectedCarId) {
      newParams.selectedCar = selectedCarId == -1 ? 'all' : selectedCarId;
    }

    // Update date range params
    if (dateRange.to) {
      newParams.to = formatDayjs(dateRange.to);
    }

    if (dateRange.from) {
      newParams.from = formatDayjs(dateRange.from);
    }

    // Only update if something actually changed
    if (newParams !== search) {
      navigate({ search: newParams });
    }
  }, [selectedCarId, dateRange, search, navigate]);
}
