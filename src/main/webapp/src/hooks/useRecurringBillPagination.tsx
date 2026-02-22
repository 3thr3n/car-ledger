import { useEffect, useState } from 'react';
import { getAllRecurringBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export default function useRecurringBillPagination(
  carId: number,
  page?: GridPaginationModel,
) {
  const [pagination, setPagination] = useState<GridPaginationModel>(
    page ?? { page: 0, pageSize: 20 },
  );
  const [sort, setSort] = useState<GridSortModel>();

  const options = getAllRecurringBillsOptions({
    path: {
      carId,
    },
    query: {
      page: pagination.page + 1,
      size: pagination.pageSize,
    },
    client: localClient,
  });

  const { data, refetch } = useQuery({
    ...options,
    placeholderData: keepPreviousData,
  });

  useEffect(() => {
    console.log('sort:', sort);
  }, [sort]);

  return { data, refetch, setPagination, setSort, pagination };
}
