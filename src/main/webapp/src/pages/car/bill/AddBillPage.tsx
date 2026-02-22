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
import { useTranslation } from 'react-i18next';

export interface AddBillPageProps {
  id: number;
  type?: BillType;
}

export default function AddBillPage({
  id,
  type: selectedType,
}: AddBillPageProps) {
  const { t } = useTranslation();

  const [type, setType] = useState<BillType>(selectedType ?? BillType.FUEL);

  return (
    <CarLedgerPage id="AddBillPage">
      <Container>
        <CarLedgerPageHeader
          title={`${t('app.car.add.title')} ${t('app.car.types.' + type)}`}
        />

        {!selectedType && (
          <Card sx={{ mb: 3 }}>
            <CardContent>
              <TextField
                select
                value={type}
                onChange={(e) => setType(e.target.value as BillType)}
                sx={{ minWidth: 200 }}
              >
                {Object.values(BillType).map((option) => (
                  <MenuItem key={option} value={option}>
                    {t(`app.car.types.${option}`)}
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
