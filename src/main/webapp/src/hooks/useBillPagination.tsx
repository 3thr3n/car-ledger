import {useState} from "react";
import {getAllBillsOptions} from "@/generated/@tanstack/react-query.gen";
import {localClient} from "@/utils/QueryClient";
import {GridPaginationModel} from "@mui/x-data-grid";
import {keepPreviousData, useQuery} from "@tanstack/react-query";

export default function useBillPagination(carId: number, page?: GridPaginationModel) {
    const [pagination, setPagination] = useState<GridPaginationModel>(page ?? {page: 0, pageSize: 20})

    const options = getAllBillsOptions({
        path: {
            carId
        },
        query: {
            page: pagination.page + 1,
            size: pagination.pageSize
        },
        client: localClient,
    });

    const {data, refetch} = useQuery({
        ...options,
        placeholderData: keepPreviousData
    });


    return {data, refetch, pagination, setPagination}
}