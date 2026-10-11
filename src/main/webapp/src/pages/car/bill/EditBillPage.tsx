import { Box, Card, Skeleton, Stack } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import {
  getFuelBillOptions,
  getMaintenanceBillOptions,
  getMiscellaneousBillOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import PageHeader from '@/components/ui/PageHeader';
import TypeChip from '@/components/ui/TypeChip';
import FuelForm from '@/components/bill/FuelForm';
import MaintenanceForm from '@/components/bill/MaintenanceForm';
import MiscellaneousForm from '@/components/bill/MiscellaneousForm';
import ErrorPage from '@/pages/ErrorPage';

export type EditableBillType =
  | BillType.FUEL
  | BillType.MAINTENANCE
  | BillType.MISCELLANEOUS;

export interface EditBillPageProps {
  carId: number;
  billId: number;
  type: EditableBillType;
}

export default function EditBillPage({
  carId,
  billId,
  type,
}: EditBillPageProps) {
  const { t } = useTranslation();
  const path = { carId, billId };

  const fuel = useQuery({
    ...getFuelBillOptions({ client: localClient, path }),
    enabled: type === BillType.FUEL,
  });
  const maintenance = useQuery({
    ...getMaintenanceBillOptions({ client: localClient, path }),
    enabled: type === BillType.MAINTENANCE,
  });
  const miscellaneous = useQuery({
    ...getMiscellaneousBillOptions({ client: localClient, path }),
    enabled: type === BillType.MISCELLANEOUS,
  });

  const query = {
    FUEL: fuel,
    MAINTENANCE: maintenance,
    MISCELLANEOUS: miscellaneous,
  }[type];

  if (query.isError) {
    return <ErrorPage error={query.error} />;
  }

  return (
    <Box id="EditBillPage" sx={{ maxWidth: 860 }}>
      <PageHeader
        title={t('bill.edit.title')}
        subtitle={t('bill.edit.subtitle')}
        actions={<TypeChip type={type} />}
      />
      <Card sx={{ p: { xs: 2, sm: 3 } }}>
        {query.isLoading ? (
          <Stack spacing={2}>
            {[0, 1, 2].map((i) => (
              <Skeleton key={i} variant="rounded" height={56} />
            ))}
          </Stack>
        ) : (
          <>
            {/* keyed, so the form is initialized with the loaded bill */}
            {fuel.data && (
              <FuelForm key={billId} carId={carId} bill={fuel.data} />
            )}
            {maintenance.data && (
              <MaintenanceForm
                key={billId}
                carId={carId}
                bill={maintenance.data}
              />
            )}
            {miscellaneous.data && (
              <MiscellaneousForm
                key={billId}
                carId={carId}
                bill={miscellaneous.data}
              />
            )}
          </>
        )}
      </Card>
    </Box>
  );
}
