import {
  Box,
  Card,
  CardContent,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import {
  DataGrid,
  GridColDef,
  GridPaginationModel,
  GridSortModel,
} from '@mui/x-data-grid';
import { BillPojo } from '@/generated';

export interface FuelTableProps {
  onDelete: (id: number) => void;
  setPagination: (pagination: GridPaginationModel) => void;
  setSortModel: (sort: GridSortModel) => void;
  isMobile?: boolean;
  totalBills: number;
  bills: BillPojo[];
}

export default function FuelTable({
  onDelete,
  setPagination,
  setSortModel,
  isMobile,
  totalBills,
  bills,
}: FuelTableProps) {
  if (isMobile) {
    return (
      <Stack spacing={2}>
        {bills.map((bill) => (
          <Card key={bill.id}>
            <CardContent>
              <Typography variant="subtitle1">{bill.day}</Typography>
              <Typography variant="body2">
                Distance: {bill.distance} km
              </Typography>
              <Typography variant="body2">Fuel: {bill.unit} L</Typography>
              <Typography variant="body2">
                Price Paid: {Number(bill.calculatedPrice ?? 0).toFixed(2)} €
              </Typography>
              <Typography variant="body2">
                Consumption: {Number(bill.calculated ?? 0).toFixed(2)} l/100km
              </Typography>
            </CardContent>
          </Card>
        ))}
      </Stack>
    );
  }

  const columns: GridColDef[] = [
    { field: 'day', headerName: 'Day', flex: 1 },
    { field: 'distance', headerName: 'Distance (km)', flex: 1 },
    { field: 'unit', headerName: 'Unit (L)', flex: 1 },
    { field: 'pricePerUnit', headerName: 'PPU (ct)', flex: 1 },
    { field: 'estimate', headerName: 'Estimate (l/100km)', flex: 1 },
    { field: 'calculated', headerName: 'Fuel Used (l/100km)', flex: 1 },
    { field: 'calculatedPrice', headerName: 'Price Paid (€)', flex: 1 },
    {
      field: 'actions',
      headerName: '',
      sortable: false,
      flex: 0.4,
      renderCell: (params) => (
        <IconButton color="error" onClick={() => onDelete(params.row.id)}>
          <DeleteIcon />
        </IconButton>
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
      />
    </Box>
  );
}
