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
 * Paged bills of one kind, filterable by the years the API reports for that kind.
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
      },
    }),
    placeholderData: keepPreviousData,
  });

  const setYear = (value: number | undefined) => {
    setYearState(value);
    setPageModel((model) => ({ ...model, page: 0 }));
  };

  return {
    years: [...(years.data ?? [])].sort((a, b) => b - a),

    year,
    setYear,
    pageModel,
    setPageModel,
    rows: (bills.data?.data ?? []) as RowMap[K][],
    total: bills.data?.total ?? 0,
    isLoading: bills.isLoading,
    isFetching: bills.isFetching,
    isError: bills.isError,
  };
}
