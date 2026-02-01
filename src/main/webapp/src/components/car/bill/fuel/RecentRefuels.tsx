import { NavigateOptions } from '@tanstack/router-core';
import { Box, Button, Card, CardContent, Divider, Grid } from '@mui/material';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import FuelPreviewTable from '@/components/car/bill/fuel/FuelPreviewTable';
import { useTranslation } from 'react-i18next';

export interface RecentRefuelsProps {
  isMobile: boolean;
  id: string;
  navigate: (opt: NavigateOptions) => void;
  reloadToken: number;
}

export default function RecentRefuels({
  isMobile,
  id,
  navigate,
  reloadToken,
}: RecentRefuelsProps) {
  const { t } = useTranslation();
  const goToAll = () => navigate({ to: '/car/$id/bill/fuel', params: { id } });

  if (isMobile) {
    return (
      <>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent
              sx={{
                display: 'flex',
              }}
            >
              <CarLedgerSubPageHeader
                title={t('app.car.fuel.recentTitle')}
                isCardHeader
              />
              <Box flexGrow={1} />
              <Button size="small" onClick={goToAll}>
                {t('app.car.common.showMore')} →
              </Button>
            </CardContent>
          </Card>
        </Grid>
      </>
    );
  }
  return (
    <>
      {/* fuel bills */}
      <Grid size={{ xs: 12, md: 8 }}>
        <Card>
          <CardContent>
            <CarLedgerSubPageHeader
              title={t('app.car.fuel.recentTitle')}
              isCardHeader
            />
            <Divider sx={{ mb: 2 }} />
            <FuelPreviewTable
              id={id}
              onSeeMore={goToAll}
              reload={reloadToken}
            />
          </CardContent>
        </Card>
      </Grid>
    </>
  );
}
