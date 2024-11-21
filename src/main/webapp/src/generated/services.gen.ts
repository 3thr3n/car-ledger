// This file is auto-generated by @hey-api/openapi-ts

import { createClient, createConfig, type Options, formDataBodySerializer } from '@hey-api/client-fetch';
import type { CallbackError, CallbackResponse, LoginError, LoginResponse, AddNewBillData, AddNewBillError, AddNewBillResponse, GetAllBillsData, GetAllBillsError, GetAllBillsResponse, DeleteBillData, DeleteBillError, DeleteBillResponse, GetMyCarsError, GetMyCarsResponse, CreateCarData, CreateCarError, CreateCarResponse, GetMyCarData, GetMyCarError, GetMyCarResponse, ImportCsvData, ImportCsvError, ImportCsvResponse, GetMyselfError, GetMyselfResponse } from './types.gen';

export const client = createClient(createConfig());

/**
 * This only for redirect purposes of oauth!
 */
export const callback = <ThrowOnError extends boolean = false>(options?: Options<unknown, ThrowOnError>) => {
    return (options?.client ?? client).get<CallbackResponse, CallbackError, ThrowOnError>({
        ...options,
        url: '/api/auth/callback'
    });
};

/**
 * Here should the browser redirect, when 'login' is pressed
 */
export const login = <ThrowOnError extends boolean = false>(options?: Options<unknown, ThrowOnError>) => {
    return (options?.client ?? client).get<LoginResponse, LoginError, ThrowOnError>({
        ...options,
        url: '/api/auth/login'
    });
};

export const addNewBill = <ThrowOnError extends boolean = false>(options: Options<AddNewBillData, ThrowOnError>) => {
    return (options?.client ?? client).put<AddNewBillResponse, AddNewBillError, ThrowOnError>({
        ...options,
        url: '/api/bill/{carId}'
    });
};

/**
 * Gets all bills for specified car
 */
export const getAllBills = <ThrowOnError extends boolean = false>(options: Options<GetAllBillsData, ThrowOnError>) => {
    return (options?.client ?? client).get<GetAllBillsResponse, GetAllBillsError, ThrowOnError>({
        ...options,
        url: '/api/bill/{carId}/all'
    });
};

export const deleteBill = <ThrowOnError extends boolean = false>(options: Options<DeleteBillData, ThrowOnError>) => {
    return (options?.client ?? client).delete<DeleteBillResponse, DeleteBillError, ThrowOnError>({
        ...options,
        url: '/api/bill/{carId}/{billId}'
    });
};

export const getMyCars = <ThrowOnError extends boolean = false>(options?: Options<unknown, ThrowOnError>) => {
    return (options?.client ?? client).get<GetMyCarsResponse, GetMyCarsError, ThrowOnError>({
        ...options,
        url: '/api/car/my'
    });
};

export const createCar = <ThrowOnError extends boolean = false>(options?: Options<CreateCarData, ThrowOnError>) => {
    return (options?.client ?? client).put<CreateCarResponse, CreateCarError, ThrowOnError>({
        ...options,
        url: '/api/car/my'
    });
};

export const getMyCar = <ThrowOnError extends boolean = false>(options: Options<GetMyCarData, ThrowOnError>) => {
    return (options?.client ?? client).get<GetMyCarResponse, GetMyCarError, ThrowOnError>({
        ...options,
        url: '/api/car/my/{id}'
    });
};

/**
 * 	This is the description for the import of an csv of your bills.
 *
 * You need to add the csv and optionally the order in the csv (starts with 0).
 * If you're not adding the order, the default is: day, unit, pricePerUnit, distance, estimate
 * Seperator between columns is ',' (comma)
 *
 */
export const importCsv = <ThrowOnError extends boolean = false>(options: Options<ImportCsvData, ThrowOnError>) => {
    return (options?.client ?? client).post<ImportCsvResponse, ImportCsvError, ThrowOnError>({
        ...options,
        ...formDataBodySerializer,
        headers: {
            'Content-Type': null,
            ...options?.headers
        },
        url: '/api/import/{carId}'
    });
};

export const getMyself = <ThrowOnError extends boolean = false>(options?: Options<unknown, ThrowOnError>) => {
    return (options?.client ?? client).get<GetMyselfResponse, GetMyselfError, ThrowOnError>({
        ...options,
        url: '/api/user/me'
    });
};