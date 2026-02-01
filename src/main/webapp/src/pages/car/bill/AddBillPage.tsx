import { useState } from 'react';
import {
  Card,
  CardContent,
  Container,
  MenuItem,
  TextField,
  Typography,
} from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';
import AddFuelForm from '@/components/car/bill/AddFuelForm';
import CarLedgerPage from '@/components/CarLedgerPage';
// import MaintenanceBillForm from '@/components/bill/MaintenanceBillForm';
// import CreditBillForm from '@/components/bill/CreditBillForm';

export interface AddBillPageProps {
  id: number;
  navigate: (path: NavigateOptions) => void;
}

const BILL_TYPES = [
  { value: 'fuel', label: 'Fuel Bill' },
  { value: 'maintenance', label: 'Maintenance Bill' },
  { value: 'credit', label: 'Credit Payment' },
];

type BillType = 'fuel' | 'maintenance' | 'credit';
export default function AddBillPage({ id, navigate }: AddBillPageProps) {
  const [type, setType] = useState<BillType>('fuel');

  return (
    <CarLedgerPage id="AddBillPage">
      <Container>
        <Typography variant="h4" fontWeight={700} gutterBottom>
          Add New {BILL_TYPES.find((t) => t.value === type)?.label}
        </Typography>

        <Card sx={{ mb: 3 }}>
          <CardContent>
            <TextField
              select
              label="Bill Type"
              value={type}
              onChange={(e) => setType(e.target.value as BillType)}
              sx={{ minWidth: 200 }}
            >
              {BILL_TYPES.map((option) => (
                <MenuItem key={option.value} value={option.value}>
                  {option.label}
                </MenuItem>
              ))}
            </TextField>
          </CardContent>
        </Card>

        {/* Conditional form render */}
        {type === 'fuel' && <AddFuelForm carId={id} navigate={navigate} />}
        {/*{type === 'maintenance' && (*/}
        {/*  <MaintenanceBillForm carId={id} navigate={navigate} />*/}
        {/*)}*/}
        {/*{type === 'credit' && <CreditBillForm carId={id} navigate={navigate} />}*/}
      </Container>
    </CarLedgerPage>
  );
}
