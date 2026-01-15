import { Card, CardContent, Divider, Skeleton } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarOverviewOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import SingleLineStat from '@/components/base/SingleLineStat';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';

export const CarOverviewStats = ({
  carId,
  reload,
}: {
  carId: number;
  reload: number;
}) => {
  const { t } = useTranslation();
  const { data, isLoading, refetch } = useQuery({
    ...getMyCarOverviewOptions({
      client: localClient,
      path: {
        id: carId,
      },
    }),
  });

  useEffect(() => {
    if (reload != 0) {
      refetch();
    }
  }, [refetch, reload]);

  return (
    <Card sx={{ mt: 3 }}>
      <CardContent>
        <CarLedgerSubPageHeader
          title={t('app.car.stats.fuel.title')}
          isCardHeader
        />

        <Divider sx={{ mb: 2 }} />

        {isLoading ? (
          <>
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} variant="rectangular" height={60} />
            ))}
          </>
        ) : (
          <>
            <SingleLineStat
              label={t('app.car.stats.fuel.refuels') + ':'}
              value={data?.totalRefuels}
            />
            <SingleLineStat
              label={t('app.car.stats.fuel.cost') + ':'}
              value={data?.totalCost?.toFixed(2)}
              type="€"
            />
            <SingleLineStat
              label={t('app.car.stats.fuel.consumption') + ':'}
              value={data?.avgConsumption?.toFixed(2)}
              type="l/100km"
            />
          </>
        )}
      </CardContent>
    </Card>
  );
};
