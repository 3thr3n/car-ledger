import React from 'react';
import { Box } from '@mui/material';
import { DataGrid, GridColDef, GridPaginationModel } from '@mui/x-data-grid';
import { BillPojoPaged } from '@/generated';

type CarBillGridProps = {
  bills: BillPojoPaged;
  onPageChange?: (pagination: GridPaginationModel) => void;
};

export default function CarBillGrid({ bills, onPageChange }: CarBillGridProps) {
  const columns: GridColDef[] = [
    { field: 'day', headerName: 'Date', flex: 1 },
    {
      field: 'distance',
      headerName: 'Distance (km)',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(1) : '—',
    },
    {
      field: 'unit',
      headerName: 'Unit (L)',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(2) : '—',
    },
    {
      field: 'pricePerUnit',
      headerName: 'Price/unit',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(2) : '—',
    },
    {
      field: 'estimate',
      headerName: 'Estimate',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(2) : '—',
    },
    {
      field: 'calculated',
      headerName: 'Calculated',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(2) : '—',
    },
    {
      field: 'calculatedPrice',
      headerName: 'Calculated Price',
      flex: 1,
      type: 'number',
      valueFormatter: (params: number) =>
        params != null ? Number(params).toFixed(2) : '—',
    },
  ];

  return (
    <Box sx={{ width: '100%', height: 500 }}>
      <DataGrid
        sx={{
          '& .MuiDataGrid-columnHeaders': {
            minHeight: 56,
            maxHeight: 56,
            lineHeight: '56px',
          },
          '& .MuiDataGrid-columnHeaderTitle': {
            whiteSpace: 'normal',
            overflow: 'visible',
            textOverflow: 'unset',
            lineHeight: 'normal',
            paddingBottom: '2px', // ensures text doesn’t clip at bottom
          },
          '& .MuiDataGrid-columnHeader': {
            display: 'flex',
            alignItems: 'center',
          },
        }}
        rows={bills.data}
        columns={columns}
        rowCount={bills.total}
        pagination
        paginationMode="server"
        onPaginationModelChange={onPageChange}
        disableColumnSelector
        disableRowSelectionOnClick
        getRowId={(row) => row.id}
      />
    </Box>
  );
}
