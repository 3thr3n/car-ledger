import { Card, CardContent, Divider, Skeleton } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarOverviewOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import SingleLineStat from '@/components/base/SingleLineStat';
import SubPageHeader from '@/components/base/SubPageHeader';
import { useEffect } from 'react';

export const CarOverviewStats = ({
  carId,
  reload,
}: {
  carId: number;
  reload: number;
}) => {
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
        <SubPageHeader title="Fuel Statistics" isCardHeader />

        <Divider sx={{ mb: 2 }} />

        {isLoading ? (
          <>
            {[1, 2, 3].map((i) => (
              <Skeleton key={i} variant="rectangular" height={60} />
            ))}
          </>
        ) : (
          <>
            <SingleLineStat label="Total Refuels:" value={data?.totalRefuels} />
            <SingleLineStat
              label="Total Cost:"
              value={data?.totalCost?.toFixed(2)}
              type="€"
            />
            <SingleLineStat
              label="Avg Consumption:"
              value={data?.avgConsumption?.toFixed(2)}
              type="l/100km"
            />
          </>
        )}
      </CardContent>
    </Card>
  );
};
