// This file is auto-generated by @hey-api/openapi-ts

import type { Options } from '@hey-api/client-fetch';
import {
  queryOptions,
  type UseMutationOptions,
  infiniteQueryOptions,
  type InfiniteData,
} from '@tanstack/react-query';
import {
  client,
  callback,
  login,
  logout,
  addNewBill,
  getAllBills,
  deleteBill,
  getMyCars,
  createCar,
  getMyCar,
  importCsv,
  getApiStatsByCarIdAverage,
  getApiStatsByCarIdHiLo,
  getApiStatsByCarIdTotal,
  getMyself,
} from '../services.gen';
import type {
  AddNewBillData,
  AddNewBillError,
  AddNewBillResponse,
  GetAllBillsData,
  GetAllBillsError,
  GetAllBillsResponse,
  DeleteBillData,
  DeleteBillError,
  DeleteBillResponse,
  CreateCarData,
  CreateCarError,
  CreateCarResponse,
  GetMyCarData,
  ImportCsvData,
  ImportCsvError,
  ImportCsvResponse,
  GetApiStatsByCarIdAverageData,
  GetApiStatsByCarIdHiLoData,
  GetApiStatsByCarIdTotalData,
} from '../types.gen';

type QueryKey<TOptions extends Options> = [
  Pick<TOptions, 'baseUrl' | 'body' | 'headers' | 'path' | 'query'> & {
    _id: string;
    _infinite?: boolean;
  },
];

const createQueryKey = <TOptions extends Options>(
  id: string,
  options?: TOptions,
  infinite?: boolean,
): QueryKey<TOptions>[0] => {
  const params: QueryKey<TOptions>[0] = {
    _id: id,
    baseUrl: (options?.client ?? client).getConfig().baseUrl,
  } as QueryKey<TOptions>[0];
  if (infinite) {
    params._infinite = infinite;
  }
  if (options?.body) {
    params.body = options.body;
  }
  if (options?.headers) {
    params.headers = options.headers;
  }
  if (options?.path) {
    params.path = options.path;
  }
  if (options?.query) {
    params.query = options.query;
  }
  return params;
};

export const callbackQueryKey = (options?: Options) => [
  createQueryKey('callback', options),
];

export const callbackOptions = (options?: Options) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await callback({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: callbackQueryKey(options),
  });
};

export const loginQueryKey = (options?: Options) => [
  createQueryKey('login', options),
];

export const loginOptions = (options?: Options) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await login({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: loginQueryKey(options),
  });
};

export const logoutQueryKey = (options?: Options) => [
  createQueryKey('logout', options),
];

export const logoutOptions = (options?: Options) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await logout({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: logoutQueryKey(options),
  });
};

export const addNewBillMutation = (
  options?: Partial<Options<AddNewBillData>>,
) => {
  const mutationOptions: UseMutationOptions<
    AddNewBillResponse,
    AddNewBillError,
    Options<AddNewBillData>
  > = {
    mutationFn: async (localOptions) => {
      const { data } = await addNewBill({
        ...options,
        ...localOptions,
        throwOnError: true,
      });
      return data;
    },
  };
  return mutationOptions;
};

export const getAllBillsQueryKey = (options: Options<GetAllBillsData>) => [
  createQueryKey('getAllBills', options),
];

export const getAllBillsOptions = (options: Options<GetAllBillsData>) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getAllBills({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getAllBillsQueryKey(options),
  });
};

const createInfiniteParams = <
  K extends Pick<QueryKey<Options>[0], 'body' | 'headers' | 'path' | 'query'>,
>(
  queryKey: QueryKey<Options>,
  page: K,
) => {
  const params = queryKey[0];
  if (page.body) {
    params.body = {
      ...(queryKey[0].body as any),
      ...(page.body as any),
    };
  }
  if (page.headers) {
    params.headers = {
      ...queryKey[0].headers,
      ...page.headers,
    };
  }
  if (page.path) {
    params.path = {
      ...queryKey[0].path,
      ...page.path,
    };
  }
  if (page.query) {
    params.query = {
      ...queryKey[0].query,
      ...page.query,
    };
  }
  return params as unknown as typeof page;
};

export const getAllBillsInfiniteQueryKey = (
  options: Options<GetAllBillsData>,
): QueryKey<Options<GetAllBillsData>> => [
  createQueryKey('getAllBills', options, true),
];

export const getAllBillsInfiniteOptions = (
  options: Options<GetAllBillsData>,
) => {
  return infiniteQueryOptions<
    GetAllBillsResponse,
    GetAllBillsError,
    InfiniteData<GetAllBillsResponse>,
    QueryKey<Options<GetAllBillsData>>,
    | number
    | Pick<
        QueryKey<Options<GetAllBillsData>>[0],
        'body' | 'headers' | 'path' | 'query'
      >
  >(
    // @ts-expect-error meep
    {
      queryFn: async ({ pageParam, queryKey, signal }) => {
        // @ts-expect-error meep
        const page: Pick<
          QueryKey<Options<GetAllBillsData>>[0],
          'body' | 'headers' | 'path' | 'query'
        > =
          typeof pageParam === 'object'
            ? pageParam
            : {
                query: {
                  page: pageParam,
                },
              };
        const params = createInfiniteParams(queryKey, page);
        const { data } = await getAllBills({
          ...options,
          ...params,
          signal,
          throwOnError: true,
        });
        return data;
      },
      queryKey: getAllBillsInfiniteQueryKey(options),
    },
  );
};

export const deleteBillMutation = (
  options?: Partial<Options<DeleteBillData>>,
) => {
  const mutationOptions: UseMutationOptions<
    DeleteBillResponse,
    DeleteBillError,
    Options<DeleteBillData>
  > = {
    mutationFn: async (localOptions) => {
      const { data } = await deleteBill({
        ...options,
        ...localOptions,
        throwOnError: true,
      });
      return data;
    },
  };
  return mutationOptions;
};

export const getMyCarsQueryKey = (options?: Options) => [
  createQueryKey('getMyCars', options),
];

export const getMyCarsOptions = (options?: Options) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getMyCars({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getMyCarsQueryKey(options),
  });
};

export const createCarMutation = (
  options?: Partial<Options<CreateCarData>>,
) => {
  const mutationOptions: UseMutationOptions<
    CreateCarResponse,
    CreateCarError,
    Options<CreateCarData>
  > = {
    mutationFn: async (localOptions) => {
      const { data } = await createCar({
        ...options,
        ...localOptions,
        throwOnError: true,
      });
      return data;
    },
  };
  return mutationOptions;
};

export const getMyCarQueryKey = (options: Options<GetMyCarData>) => [
  createQueryKey('getMyCar', options),
];

export const getMyCarOptions = (options: Options<GetMyCarData>) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getMyCar({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getMyCarQueryKey(options),
  });
};

export const importCsvQueryKey = (options: Options<ImportCsvData>) => [
  createQueryKey('importCsv', options),
];

export const importCsvOptions = (options: Options<ImportCsvData>) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await importCsv({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: importCsvQueryKey(options),
  });
};

export const importCsvMutation = (
  options?: Partial<Options<ImportCsvData>>,
) => {
  const mutationOptions: UseMutationOptions<
    ImportCsvResponse,
    ImportCsvError,
    Options<ImportCsvData>
  > = {
    mutationFn: async (localOptions) => {
      const { data } = await importCsv({
        ...options,
        ...localOptions,
        throwOnError: true,
      });
      return data;
    },
  };
  return mutationOptions;
};

export const getApiStatsByCarIdAverageQueryKey = (
  options: Options<GetApiStatsByCarIdAverageData>,
) => [createQueryKey('getApiStatsByCarIdAverage', options)];

export const getApiStatsByCarIdAverageOptions = (
  options: Options<GetApiStatsByCarIdAverageData>,
) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getApiStatsByCarIdAverage({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getApiStatsByCarIdAverageQueryKey(options),
  });
};

export const getApiStatsByCarIdHiLoQueryKey = (
  options: Options<GetApiStatsByCarIdHiLoData>,
) => [createQueryKey('getApiStatsByCarIdHiLo', options)];

export const getApiStatsByCarIdHiLoOptions = (
  options: Options<GetApiStatsByCarIdHiLoData>,
) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getApiStatsByCarIdHiLo({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getApiStatsByCarIdHiLoQueryKey(options),
  });
};

export const getApiStatsByCarIdTotalQueryKey = (
  options: Options<GetApiStatsByCarIdTotalData>,
) => [createQueryKey('getApiStatsByCarIdTotal', options)];

export const getApiStatsByCarIdTotalOptions = (
  options: Options<GetApiStatsByCarIdTotalData>,
) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getApiStatsByCarIdTotal({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getApiStatsByCarIdTotalQueryKey(options),
  });
};

export const getMyselfQueryKey = (options?: Options) => [
  createQueryKey('getMyself', options),
];

export const getMyselfOptions = (options?: Options) => {
  return queryOptions({
    queryFn: async ({ queryKey, signal }) => {
      const { data } = await getMyself({
        ...options,
        ...queryKey[0],
        signal,
        throwOnError: true,
      });
      return data;
    },
    queryKey: getMyselfQueryKey(options),
  });
};
