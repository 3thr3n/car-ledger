import { BillPojo } from '@/generated';
import { Box, IconButton } from '@mui/material';
import {
  DataGrid,
  GridColDef,
  GridPaginationModel,
  GridRenderCellParams,
} from '@mui/x-data-grid';
import { DeleteOutline } from '@mui/icons-material';
import useBillPagination from '@/hooks/useBillPagination';
import { forwardRef, useImperativeHandle } from 'react';

export interface BillTableProps {
  carId: number;
  delete: (id: number | string) => void;
}

export interface BillTableRef {
  refresh: () => void;
}

const BillTable = forwardRef<BillTableRef, BillTableProps>((props, ref) => {
  const { refetch, data, setPagination } = useBillPagination(props.carId);

  const columns: GridColDef<BillPojo>[] = [
    {
      field: 'id',
      headerName: 'ID',
      width: 80,
      hideSortIcons: true,
    },
    {
      field: 'day',
      headerName: 'Day',
      align: 'center',
      hideSortIcons: true,
      type: 'date',
      flex: 1,
      minWidth: 100,
      valueGetter: (value) => new Date(value),
    },
    {
      field: 'distance',
      headerName: 'Distance',
      align: 'right',
      valueFormatter: (value: number) => {
        return `${value} km`;
      },
      hideSortIcons: true,
    },
    {
      field: 'unit',
      headerName: 'Unit',
      align: 'right',
      valueFormatter: (value: number) => {
        return `${value} l`;
      },
      hideSortIcons: true,
    },
    {
      field: 'pricePerUnit',
      headerName: 'PPU',
      description: 'Price payed per unit',
      align: 'right',
      valueFormatter: (value: number) => {
        return `${value} ct`;
      },
      hideSortIcons: true,
    },
    {
      field: 'estimate',
      headerName: 'Estimate',
      description: 'Estimated fuel consumption of the car',
      align: 'right',
      width: 130,
      valueFormatter: (value: number) => {
        return `${value} l/100km`;
      },
      hideSortIcons: true,
    },
    {
      field: 'calculated',
      headerName: 'Fuel used',
      description:
        'Calculated fuel consumption (based on the distance and unit)',
      align: 'right',
      width: 130,
      valueFormatter: (value: number) => {
        return `${value} l/100km`;
      },
      hideSortIcons: true,
    },
    {
      field: 'calculatedPrice',
      headerName: 'Price payed',
      description: 'Calculated price payed (based on unit and price per unit)',
      align: 'right',
      valueFormatter: (value: number) => {
        return `${value} €`;
      },
      hideSortIcons: true,
    },
    {
      field: 'delete',
      headerName: '',
      width: 30,
      hideSortIcons: true,
      hideable: false,
      disableColumnMenu: true,
      align: 'center',
      renderCell: (params: GridRenderCellParams<BillPojo>) => (
        <IconButton onClick={() => deleteBill(params.id)}>
          <DeleteOutline color="error" />
        </IconButton>
      ),
    },
  ];

  const deleteBill = (billId: number | string) => {
    console.log('delete bill', billId);
    props.delete(billId);
  };

  const updatePagination = (pagination: GridPaginationModel) => {
    setPagination(pagination);
  };

  useImperativeHandle(ref, () => ({
    async refresh() {
      await refetch();
    },
  }));

  return (
    <Box maxWidth={'100%'} minWidth={400} padding={4} pt={0} overflow={'auto'}>
      <DataGrid
        columns={columns}
        rows={data?.data}
        rowCount={data?.total ?? 0}
        paginationMode="server"
        pageSizeOptions={[10, 20, 50]}
        onPaginationModelChange={updatePagination}
        initialState={{
          sorting: {
            sortModel: [
              {
                field: 'day',
                sort: 'desc',
              },
            ],
          },
          pagination: {
            paginationModel: {
              page: data?.page ? data.page - 1 : 0,
              pageSize: data?.size ?? 20,
            },
          },
        }}
        columnVisibilityModel={{
          id: false,
        }}
      />
    </Box>
  );
});

export default BillTable;
