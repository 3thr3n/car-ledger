import { useState } from 'react';
import {
  Card,
  CardContent,
  Container,
  MenuItem,
  TextField,
} from '@mui/material';
import AddFuelForm from '@/components/car/bill/fuel/AddFuelForm';
import CarLedgerPage from '@/components/CarLedgerPage';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { BillType } from '@/generated';
import AddMaintenanceForm from '@/components/car/bill/maintenance/AddMaintenanceForm';
import AddMiscellaneousForm from '@/components/car/bill/miscellaneous/AddMiscellaneousForm';
import AddRecurringForm from '@/components/car/bill/recurring/AddRecurringForm';

export interface AddBillPageProps {
  id: number;
  type?: BillType;
}

const BILL_TYPES: { value: BillType; label: string }[] = [
  { value: BillType.FUEL, label: 'Fuel Bill' },
  { value: BillType.MAINTENANCE, label: 'Maintenance Bill' },
  { value: BillType.MISCELLANEOUS, label: 'Miscellaneous Bill' },
  { value: BillType.RECURRING, label: 'Recurring Bill' },
];

export default function AddBillPage({
  id,
  type: selectedType,
}: AddBillPageProps) {
  const [type, setType] = useState<BillType>(selectedType ?? BillType.FUEL);

  return (
    <CarLedgerPage id="AddBillPage">
      <Container>
        <CarLedgerPageHeader
          title={`Add New ${BILL_TYPES.find((t) => t.value === type)?.label}`}
        />

        {!selectedType && (
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
        )}

        {/* Conditional form render */}
        {type === 'FUEL' && <AddFuelForm carId={id} />}
        {type === 'MAINTENANCE' && <AddMaintenanceForm carId={id} />}
        {type === 'MISCELLANEOUS' && <AddMiscellaneousForm carId={id} />}
        {type === 'RECURRING' && <AddRecurringForm carId={id} />}
      </Container>
    </CarLedgerPage>
  );
}
