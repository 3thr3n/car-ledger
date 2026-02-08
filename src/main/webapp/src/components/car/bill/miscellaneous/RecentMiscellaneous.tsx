import { NavigateOptions } from '@tanstack/router-core';
import { Box, Button, Card, CardContent, Divider, Grid } from '@mui/material';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import { useTranslation } from 'react-i18next';
import MiscellaneousPreviewTable from '@/components/car/bill/miscellaneous/MiscellaneousPreviewTable';

export interface RecentMaintenanceProps {
  isMobile: boolean;
  id: string;
  navigate: (opt: NavigateOptions) => void;
  reloadToken: number;
}

export default function RecentMiscellaneous({
  isMobile,
  id,
  navigate,
  reloadToken,
}: RecentMaintenanceProps) {
  const { t } = useTranslation();
  const goToAll = () =>
    navigate({ to: '/car/$id/bill/miscellaneous', params: { id } });

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
              title={t('app.car.miscellaneous.recentTitle')}
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
            title={t('app.car.miscellaneous.recentTitle')}
            isCardHeader
          />
          <Divider sx={{ mb: 2 }} />
          <MiscellaneousPreviewTable
            id={id}
            onSeeMore={goToAll}
            reload={reloadToken}
          />
        </CardContent>
      </Card>
    </Grid>
  );
}
