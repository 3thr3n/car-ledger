import { useState } from 'react';
import {
  Card,
  CardContent,
  Container,
  MenuItem,
  TextField,
} from '@mui/material';
import AddFuelForm from '@/components/car/bill/AddFuelForm';
import CarLedgerPage from '@/components/CarLedgerPage';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { BillType } from '@/generated';
import AddMaintenanceForm from '@/components/car/bill/AddMaintenanceForm';

export interface AddBillPageProps {
  id: number;
}

const BILL_TYPES: { value: BillType; label: string }[] = [
  { value: 'FUEL', label: 'Fuel Bill' },
  { value: 'MAINTENANCE', label: 'Maintenance Bill' },
];

export default function AddBillPage({ id }: AddBillPageProps) {
  const [type, setType] = useState<BillType>('FUEL');

  return (
    <CarLedgerPage id="AddBillPage">
      <Container>
        <CarLedgerPageHeader
          title={`Add New ${BILL_TYPES.find((t) => t.value === type)?.label}`}
        />

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
        {type === 'FUEL' && <AddFuelForm carId={id} />}
        {type === 'MAINTENANCE' && <AddMaintenanceForm carId={id} />}
        {/*{type === 'credit' && <CreditBillForm carId={id} navigate={navigate} />}*/}
      </Container>
    </CarLedgerPage>
  );
}
