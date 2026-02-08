import { NavigateOptions } from '@tanstack/router-core';
import { Box, Button, Card, CardContent, Divider, Grid } from '@mui/material';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import { useTranslation } from 'react-i18next';
import MaintenancePreviewTable from '@/components/car/bill/maintenance/MaintenancePreviewTable';

export interface RecentMaintenanceProps {
  isMobile: boolean;
  id: string;
  navigate: (opt: NavigateOptions) => void;
  reloadToken: number;
}

export default function RecentMaintenance({
  isMobile,
  id,
  navigate,
  reloadToken,
}: RecentMaintenanceProps) {
  const { t } = useTranslation();
  const goToAll = () =>
    navigate({ to: '/car/$id/bill/maintenance', params: { id } });

  if (isMobile) {
    return (
      <Grid size={12}>
        <Card>
          <CardContent
            sx={{
              display: 'flex',
            }}
          >
            <CarLedgerSubPageHeader
              title={t('app.car.maintenance.recentTitle')}
              isCardHeader
            />
            <Box flexGrow={1} />
            <Button size="small" onClick={goToAll}>
              {t('app.car.common.showMore')} →
            </Button>
          </CardContent>
        </Card>
      </Grid>
    );
  }
  return (
    <Grid size={12}>
      <Card>
        <CardContent>
          <CarLedgerSubPageHeader
            title={t('app.car.maintenance.recentTitle')}
            isCardHeader
          />
          <Divider sx={{ mb: 2 }} />
          <MaintenancePreviewTable
            id={id}
            onSeeMore={goToAll}
            reload={reloadToken}
          />
        </CardContent>
      </Card>
    </Grid>
  );
}
