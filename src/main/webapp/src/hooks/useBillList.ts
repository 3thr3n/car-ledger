import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import {
  getAllBillsOptions,
  getAllBillYearsOptions,
  getAllFuelBillsOptions,
  getAllFuelBillYearsOptions,
  getAllMaintenanceBillsOptions,
  getAllMaintenanceBillYearsOptions,
  getAllMiscellaneousBillsOptions,
  getAllMiscellaneousBillYearsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import {
  Bill,
  FuelBill,
  MaintenanceBill,
  MiscellaneousBill,
  SortOrder,
} from '@/generated';

export type ListKind = 'all' | 'fuel' | 'maintenance' | 'miscellaneous';

interface RowMap {
  all: Bill;
  fuel: FuelBill;
  maintenance: MaintenanceBill;
  miscellaneous: MiscellaneousBill;
}

export interface PageModel {
  /** zero based, as used by the DataGrid */
  page: number;
  pageSize: number;
}

export interface SortModel {
  /** field of the row, sorted by on the server */
  field: string;
  sort: 'asc' | 'desc';
}

/** the order of the API without a sort model: newest first */
export const DEFAULT_SORT: SortModel = { field: 'date', sort: 'desc' };

const listOptions = {
  all: getAllBillsOptions,
  fuel: getAllFuelBillsOptions,
  maintenance: getAllMaintenanceBillsOptions,
  miscellaneous: getAllMiscellaneousBillsOptions,
};

const yearOptions = {
  all: getAllBillYearsOptions,
  fuel: getAllFuelBillYearsOptions,
  maintenance: getAllMaintenanceBillYearsOptions,
  miscellaneous: getAllMiscellaneousBillYearsOptions,
};

/**
 * Paged bills of one kind, sorted on the server, filterable by the years the API reports for
 * that kind.
 */
export default function useBillList<K extends ListKind>(
  kind: K,
  carId: number,
  initialPageSize = 20,
) {
  const [year, setYearState] = useState<number | undefined>();
  const [pageModel, setPageModel] = useState<PageModel>({
    page: 0,
    pageSize: initialPageSize,
  });
  const [sortModel, setSortModelState] = useState<SortModel>(DEFAULT_SORT);

  const years = useQuery(
    (yearOptions[kind] as typeof getAllBillYearsOptions)({
      client: localClient,
      path: { carId },
    }),
  );

  const bills = useQuery({
    // all list endpoints share the same parameters, only the row type differs
    ...(listOptions[kind] as typeof getAllBillsOptions)({
      client: localClient,
      path: { carId },
      query: {
        // the API starts paging at 1
        page: pageModel.page + 1,
        size: pageModel.pageSize,
        year,
        sort: sortModel.field,
        order: sortModel.sort === 'asc' ? SortOrder.ASC : SortOrder.DESC,
      },
    }),
    placeholderData: keepPreviousData,
  });

  const setYear = (value: number | undefined) => {
    setYearState(value);
    setPageModel((model) => ({ ...model, page: 0 }));
  };

  // a new order starts at the first page again
  const setSortModel = (value: SortModel | undefined) => {
    setSortModelState(value ?? DEFAULT_SORT);
    setPageModel((model) => ({ ...model, page: 0 }));
  };

  return {
    years: [...(years.data ?? [])].sort((a, b) => b - a),

    year,
    setYear,
    pageModel,
    setPageModel,
    sortModel,
    setSortModel,
    rows: (bills.data?.data ?? []) as RowMap[K][],
    total: bills.data?.total ?? 0,
    isLoading: bills.isLoading,
    isFetching: bills.isFetching,
    isError: bills.isError,
  };
}
