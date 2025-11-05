import {
  Box,
  Card,
  CardContent,
  CircularProgress,
  IconButton,
  Stack,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import DeleteIcon from '@mui/icons-material/Delete';
import { useState } from 'react';
import { BillPojo } from '@/generated';
import YearSelection from '@/components/car/fuel/YearSelection';
import { useQuery } from '@tanstack/react-query';
import { getAllBillYearsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useBillPagination from '@/hooks/useBillPagination';

interface CarBillOverviewProps {
  id: number;
  onDelete: (id: number) => void;
}

export default function AllViewPage({ id, onDelete }: CarBillOverviewProps) {
  const isMobile = useMediaQuery('(max-width:900px)');

  const {
    data: yearData,
    isError: isYearError,
    isLoading: isYearLoading,
  } = useQuery({
    ...getAllBillYearsOptions({
      client: localClient,
      path: {
        carId: id,
      },
    }),
  });

  const { setYear, data: billData, setPagination } = useBillPagination(id);

  const years: number[] = yearData ?? [];
  const bills: BillPojo[] = billData?.data ?? [];

  const currentYear = new Date().getFullYear();
  const [selectedYear, setSelectedYear] = useState<number>(
    years.includes(currentYear) ? currentYear : -1,
  );

  async function updateYear(year: number) {
    setSelectedYear(year);
    setYear(year);
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

  if (isYearLoading || isYearError) {
    return <CircularProgress />;
  }

  // 📱 Mobile view: cards
  if (isMobile) {
    return (
      <Box sx={{ p: 2 }}>
        <Typography variant="h6" gutterBottom>
          Fuel Entries
        </Typography>

        <YearSelection
          years={years}
          selectedYear={selectedYear}
          setSelectedYear={updateYear}
          isMobile
        />

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
      </Box>
    );
  }

  // 💻 Desktop view: sidebar selector + table
  return (
    <Box sx={{ display: 'flex', gap: 3 }}>
      {/* Year Selector */}
      <YearSelection
        years={years}
        selectedYear={selectedYear}
        setSelectedYear={updateYear}
      />

      {/* DataGrid */}
      <Box sx={{ flex: 1, height: 700 }}>
        <DataGrid
          rows={bills}
          getRowId={(r) => r.id}
          rowCount={billData?.total ?? 0}
          columns={columns}
          pagination
          pageSizeOptions={[10, 20, 50]}
          paginationMode="server"
          sortingMode="server"
          onPaginationModelChange={(model) => setPagination(model)}
          // onSortModelChange={(model) => setSortModel(model)}
          initialState={{
            pagination: { paginationModel: { pageSize: 20, page: 0 } },
          }}
          disableRowSelectionOnClick
        />
      </Box>
    </Box>
  );
}
