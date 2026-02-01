import { useEffect, useState } from 'react';
import { getAllFuelBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { GridPaginationModel, GridSortModel } from '@mui/x-data-grid';
import { keepPreviousData, useQuery } from '@tanstack/react-query';

export default function useFuelBillPagination(
  carId: number,
  page?: GridPaginationModel,
) {
  const [pagination, setPagination] = useState<GridPaginationModel>(
    page ?? { page: 0, pageSize: 20 },
  );
  const [sort, setSort] = useState<GridSortModel>();
  const [year, setYear] = useState<number>(-1);

  const options = getAllFuelBillsOptions({
    path: {
      carId,
    },
    query: {
      page: pagination.page + 1,
      size: pagination.pageSize,
      // sort: sort,
      year: year === -1 ? undefined : year,
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

  return { data, refetch, setPagination, setYear, setSort, pagination };
}
