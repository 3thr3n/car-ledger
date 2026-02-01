import { Box, Card, CardContent, Stack, Typography } from '@mui/material';
import {
  DataGrid,
  GridColDef,
  GridPaginationModel,
  GridSortModel,
} from '@mui/x-data-grid';
import { MaintenanceBill } from '@/generated';
import { useTranslation } from 'react-i18next';
import CarLedgerDeleteIcon from '@/components/CarLedgerDeleteIcon';

export interface MaintenanceTableProps {
  onDelete: (id: number) => void;
  setPagination: (pagination: GridPaginationModel) => void;
  setSortModel: (sort: GridSortModel) => void;
  isMobile?: boolean;
  totalBills: number;
  bills: MaintenanceBill[];
}

export default function MaintenanceTable({
  onDelete,
  setPagination,
  setSortModel,
  isMobile,
  totalBills,
  bills,
}: MaintenanceTableProps) {
  const { t } = useTranslation();

  if (isMobile) {
    return (
      <Stack spacing={2}>
        {bills.map((bill) => (
          <Card key={bill.id}>
            <CardContent>
              <Typography variant="subtitle1">{bill.date}</Typography>
              <Typography variant="body2">
                {t('app.car.maintenance.table.workshop')}: {bill.workshop}
              </Typography>
              <Typography variant="body2">
                {t('app.car.maintenance.table.odometer')}: {bill.odometer} km
              </Typography>
              <Typography variant="body2">
                {t('app.car.fuel.table.total')}:{' '}
                {Number(bill.total ?? 0).toFixed(2)} €
              </Typography>
            </CardContent>
          </Card>
        ))}
      </Stack>
    );
  }

  const columns: GridColDef[] = [
    { field: 'date', headerName: t('app.car.common.date'), flex: 1 },
    {
      field: 'workshop',
      headerName: `${t('app.car.maintenance.table.workshop')}`,
      flex: 1,
    },
    {
      field: 'odometer',
      headerName: `${t('app.car.common.odometer')} (km)`,
      flex: 1,
    },
    {
      field: 'total',
      headerName: `${t('app.car.common.total')} (€)`,
      flex: 1,
    },
    {
      field: 'actions',
      headerName: '',
      sortable: false,
      flex: 0.4,
      renderCell: (params) => (
        <CarLedgerDeleteIcon onDelete={() => onDelete(params.row.id)} />
      ),
    },
  ];

  return (
    <Box sx={{ flex: 1, height: 700 }}>
      <DataGrid
        rows={bills}
        getRowId={(r) => r.id}
        rowCount={totalBills}
        columns={columns}
        pagination
        pageSizeOptions={[10, 20, 50, 100]}
        paginationMode="server"
        sortingMode="server"
        onPaginationModelChange={(model) => setPagination(model)}
        onSortModelChange={(model) => setSortModel(model)}
        initialState={{
          pagination: { paginationModel: { pageSize: 20, page: 0 } },
        }}
        disableRowSelectionOnClick
        getRowClassName={(params) =>
          params.indexRelativeToCurrentPage % 2 === 0 ? 'even' : 'odd'
        }
      />
    </Box>
  );
}
