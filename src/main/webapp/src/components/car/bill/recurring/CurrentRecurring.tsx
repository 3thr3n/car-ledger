import { NavigateOptions } from '@tanstack/router-core';
import { Box, Button, Card, CardContent, Divider, Grid } from '@mui/material';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import { useTranslation } from 'react-i18next';
import RecurringPreviewTable from '@/components/car/bill/recurring/RecurringPreviewTable';

export interface RecentRecurringProps {
  isMobile: boolean;
  id: string;
  navigate: (opt: NavigateOptions) => void;
  reloadToken: number;
}

export default function RecentRecurring({
  isMobile,
  id,
  navigate,
  reloadToken,
}: RecentRecurringProps) {
  const { t } = useTranslation();
  const goToAll = () =>
    navigate({ to: '/car/$id/bill/recurring', params: { id } });

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
              title={t('app.car.recurring.recentTitle')}
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
            title={t('app.car.recurring.recentTitle')}
            isCardHeader
          />
          <Divider sx={{ mb: 2 }} />
          <RecurringPreviewTable
            id={id}
            onSeeMore={goToAll}
            reload={reloadToken}
          />
        </CardContent>
      </Card>
    </Grid>
  );
}
