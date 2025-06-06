// This file is auto-generated by @hey-api/openapi-ts

export type AccountPojo = {
  maxCars?: number;
  name?: string;
};

export type AverageStats = {
  pricePerUnit?: number;
  distance?: number;
  calculated?: number;
  calculatedPrice?: number;
};

export type BillInputPojo = {
  day?: LocalDate;
  distance?: number;
  unit?: number;
  pricePerUnit?: number;
  estimate?: number;
};

export type BillPojo = {
  id?: number;
  day?: LocalDate;
  distance?: number;
  unit?: number;
  pricePerUnit?: number;
  estimate?: number;
  calculated?: number;
  calculatedPrice?: number;
};

export type BillPojoPaged = {
  total?: number;
  page?: number;
  size?: number;
  data?: Array<BillPojo>;
};

export type CarInputPojo = {
  description?: string;
};

export type CarPojo = {
  id?: number;
  description?: string;
  amountBills?: number;
};

export type CsvOrder = {
  day?: number;
  unit?: number;
  pricePerUnit?: number;
  distance?: number;
  estimate?: number;
};

export type HiLo = {
  min?: number;
  max?: number;
  scale?: number;
};

export type HiLoStats = {
  calculatedPrice?: HiLo;
  calculated?: HiLo;
  distance?: HiLo;
  unit?: HiLo;
  pricePerUnit?: HiLo;
};

export type LocalDate = string;

export type MinimalStats = {
  total?: number;
  avgFuelConsumption?: number;
  minMaxFuelConsumption?: HiLo;
  avgDistance?: number;
};

export type TotalStats = {
  unit?: number;
  distance?: number;
  calculatedPrice?: number;
};

export type CallbackResponse = unknown;

export type CallbackError = unknown;

export type LoginResponse = unknown;

export type LoginError = unknown;

export type LogoutResponse = void;

export type LogoutError = unknown;

export type AddNewBillData = {
  body: BillInputPojo;
  path: {
    carId: number;
  };
};

export type AddNewBillResponse = unknown;

export type AddNewBillError = unknown;

export type GetAllBillsData = {
  path: {
    carId: number;
  };
  query?: {
    page?: number;
    size?: number;
  };
};

export type GetAllBillsResponse = BillPojoPaged;

export type GetAllBillsError = unknown;

export type DeleteBillData = {
  path: {
    billId: number;
    carId: number;
  };
};

export type DeleteBillResponse = unknown;

export type DeleteBillError = unknown;

export type GetMyCarsResponse = Array<CarPojo>;

export type GetMyCarsError = unknown;

export type CreateCarData = {
  body: CarInputPojo;
};

export type CreateCarResponse = unknown;

export type CreateCarError = unknown;

export type GetMyCarData = {
  path: {
    id: number;
  };
};

export type GetMyCarResponse = CarPojo | void;

export type GetMyCarError = unknown;

export type ImportCsvData = {
  body: {
    file?: Blob | File;
    order?: CsvOrder;
  };
  path: {
    carId: number;
  };
  query?: {
    skipHeader?: boolean;
  };
};

export type ImportCsvResponse = unknown;

export type ImportCsvError = unknown;

export type GetStatsAverageData = {
  path: {
    carId: number;
  };
  query?: {
    from?: LocalDate;
    to?: LocalDate;
  };
};

export type GetStatsAverageResponse = AverageStats;

export type GetStatsAverageError = unknown;

export type GetStatsHiLoData = {
  path: {
    carId: number;
  };
  query?: {
    from?: LocalDate;
    to?: LocalDate;
  };
};

export type GetStatsHiLoResponse = HiLoStats;

export type GetStatsHiLoError = unknown;

export type GetStatsMinimalData = {
  path: {
    carId: number;
  };
  query?: {
    from?: LocalDate;
    to?: LocalDate;
  };
};

export type GetStatsMinimalResponse = MinimalStats;

export type GetStatsMinimalError = unknown;

export type GetStatsTotalData = {
  path: {
    carId: number;
  };
  query?: {
    from?: LocalDate;
    to?: LocalDate;
  };
};

export type GetStatsTotalResponse = TotalStats;

export type GetStatsTotalError = unknown;

export type GetMyselfResponse = AccountPojo;

export type GetMyselfError = unknown;
