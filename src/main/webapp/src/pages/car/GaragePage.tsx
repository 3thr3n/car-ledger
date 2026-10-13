import { Box, Card, Grid, Skeleton, Stack, Typography } from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import OdometerValue from '@/components/ui/OdometerValue';
import useMe from '@/hooks/useMe';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import CarAvatar from '@/components/layout/CarAvatar';
import { RouterCardActionArea } from '@/components/ui/links';
import ErrorPage from '@/pages/ErrorPage';

export default function GaragePage() {
  const { t } = useTranslation();
  const { maxCars } = useMe();

  const {
    data: cars,
    isLoading,
    isError,
    error,
  } = useQuery(getMyCarsOptions({ client: localClient }));

  if (isError) {
    return <ErrorPage error={error} />;
  }

  const used = cars?.length ?? 0;
  const canAdd = used < maxCars;

  return (
    <Page id="GaragePage">
      <PageHeader
        title={t('garage.title')}
        subtitle={t('garage.subtitle')}
        eyebrow={maxCars > 0 && t('garage.slots', { used, max: maxCars })}
      />

      <Grid container spacing={2}>
        {isLoading &&
          [0, 1, 2].map((i) => (
            <Grid key={i} size={{ xs: 12, sm: 6, lg: 4 }}>
              <Skeleton
                variant="rounded"
                height={156}
                sx={{ borderRadius: 3.5 }}
              />
            </Grid>
          ))}

        {cars?.map((car) => (
          <Grid key={car.id} size={{ xs: 12, sm: 6, lg: 4 }}>
            <Card sx={{ height: '100%' }}>
              <RouterCardActionArea
                to="/car/$id"
                params={{ id: `${car.id}` }}
                sx={{ p: 2.5, height: '100%' }}
              >
                <Stack
                  direction="row"
                  spacing={2}
                  sx={{
                    alignItems: 'center',
                    mb: 2.5,
                  }}
                >
                  <CarAvatar
                    id={car.id}
                    name={car.name}
                    pictureId={car.pictureId}
                    size={48}
                  />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography variant="h6" noWrap>
                      {car.name}
                    </Typography>
                    <Typography
                      variant="body2"
                      sx={{
                        color: 'text.secondary',
                      }}
                    >
                      {car.year && t('car.built', { year: car.year })}
                    </Typography>
                  </Box>
                </Stack>
                <Stack direction="row" spacing={4}>
                  <Box>
                    <Typography
                      variant="caption"
                      sx={{
                        color: 'text.secondary',
                      }}
                    >
                      {t('car.odometer')}
                    </Typography>
                    <Typography
                      className="tabular"
                      sx={{
                        fontWeight: 600,
                      }}
                    >
                      <OdometerValue odometer={car.currentOdometer} />
                    </Typography>
                  </Box>
                  <Box>
                    <Typography
                      variant="caption"
                      sx={{
                        color: 'text.secondary',
                      }}
                    >
                      {t('garage.entriesLabel')}
                    </Typography>
                    <Typography
                      className="tabular"
                      sx={{
                        fontWeight: 600,
                      }}
                    >
                      {car.amountBills ?? 0}
                    </Typography>
                  </Box>
                </Stack>
              </RouterCardActionArea>
            </Card>
          </Grid>
        ))}

        {!isLoading && (
          <Grid size={{ xs: 12, sm: 6, lg: 4 }}>
            <Card
              sx={{
                height: '100%',
                minHeight: 156,
                borderStyle: 'dashed',
                bgcolor: 'transparent',
              }}
            >
              {canAdd ? (
                <RouterCardActionArea
                  to="/car/new"
                  sx={{
                    height: '100%',
                    display: 'grid',
                    placeItems: 'center',
                    color: 'text.secondary',
                  }}
                >
                  <Stack
                    spacing={0.5}
                    sx={{
                      alignItems: 'center',
                    }}
                  >
                    <AddRounded />
                    <Typography
                      sx={{
                        fontWeight: 600,
                      }}
                    >
                      {t('garage.add')}
                    </Typography>
                    {used === 0 && (
                      <Typography variant="body2">
                        {t('garage.emptyHint')}
                      </Typography>
                    )}
                  </Stack>
                </RouterCardActionArea>
              ) : (
                <Stack
                  sx={{
                    alignItems: 'center',
                    justifyContent: 'center',
                    height: '100%',
                    p: 3,
                  }}
                >
                  <Typography
                    variant="body2"
                    sx={{
                      color: 'text.secondary',
                      textAlign: 'center',
                    }}
                  >
                    {t('garage.limitReached')}
                  </Typography>
                </Stack>
              )}
            </Card>
          </Grid>
        )}
      </Grid>
    </Page>
  );
}
