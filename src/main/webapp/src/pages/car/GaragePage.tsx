import {
  Box,
  Button,
  Card,
  Chip,
  Grid,
  Skeleton,
  Stack,
  Theme,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import OrphanedEntries from '@/components/offline/OrphanedEntries';
import { isShared, ownCars } from '@/utils/carRole';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useMe from '@/hooks/useMe';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import CarAvatar from '@/components/layout/CarAvatar';
import OdometerWheels from '@/components/car/OdometerWheels';
import { useFormat } from '@/utils/format';
import { RouterCardActionArea } from '@/components/ui/links';
import ErrorPage from '@/pages/ErrorPage';
import useCarRequest from '@/hooks/useCarRequest';
import CarRequestDialog from '@/components/request/CarRequestDialog';
import CarRequestOutcome from '@/components/request/CarRequestOutcome';
import InvitationList from '@/components/car/access/InvitationList';

/** a car as a panel of the cluster: lit a little in the middle in the dark */
const panelSx = (theme: Theme) =>
  theme.applyStyles('dark', {
    borderRadius: '20px',
    background: `radial-gradient(120% 90% at 50% 0%, #1c2836 0%, ${theme.vars.palette.background.paper} 70%)`,
    boxShadow: `inset 0 0 0 1px ${theme.vars.palette.divider}`,
  });

/** the plate of the next car: still blank, dashed */
function EmptyPlate({ disabled }: { disabled?: boolean }) {
  return (
    <Box
      aria-hidden
      sx={{
        display: 'grid',
        placeItems: 'center',
        width: 168,
        height: 44,
        borderRadius: '6px',
        border: '2px dashed',
        borderColor: disabled ? 'text.disabled' : 'text.secondary',
        color: disabled ? 'text.disabled' : 'primary.main',
      }}
    >
      <AddRounded />
    </Box>
  );
}

export default function GaragePage() {
  const { t } = useTranslation();
  const { maxCars } = useMe();
  const format = useFormat();
  const { units } = format;
  const carRequest = useCarRequest();
  const [requesting, setRequesting] = useState(false);

  const {
    data: cars,
    isLoading,
    isError,
    error,
  } = useQuery(getMyCarsOptions({ client: localClient }));

  // the data of the last visit stays when the connection fails
  if (isError && !cars) {
    return <ErrorPage error={error} />;
  }

  const used = ownCars(cars).length;
  const canAdd = used < maxCars;

  return (
    <Page id="GaragePage">
      <PageHeader
        title={t('garage.title')}
        subtitle={t('garage.subtitle')}
        eyebrow={maxCars > 0 && t('garage.slots', { used, max: maxCars })}
      />

      <CarRequestOutcome />
      <InvitationList />
      <OrphanedEntries />

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
            <Card sx={[panelSx, { height: '100%' }]}>
              <RouterCardActionArea
                to="/car/$id"
                params={{ id: `${car.id}` }}
                sx={{
                  p: 2.5,
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'stretch',
                  gap: 2,
                }}
              >
                {/* the plate is the face of the car, the picture next to it */}
                <Stack
                  direction="row"
                  spacing={1.5}
                  sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                >
                  {/* without a registration, a plate with the initials */}
                  <CarAvatar
                    id={car.id}
                    name={car.name}
                    licensePlate={car.licensePlate}
                    size={44}
                  />
                  {car.pictureId != null && (
                    <CarAvatar
                      id={car.id}
                      name={car.name}
                      pictureId={car.pictureId}
                      size={44}
                    />
                  )}
                </Stack>
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="h6" component="h2" noWrap>
                    {car.name}
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    {[
                      isShared(car) &&
                        t('car.shared', { role: t(`car.roles.${car.role}`) }),
                      car.year && t('car.built', { year: car.year }),
                      t(`car.energyTypes.${car.energyType}`),
                    ]
                      .filter(Boolean)
                      .join(' · ')}
                  </Typography>
                </Box>
                <Stack
                  direction="row"
                  spacing={1.5}
                  sx={{
                    mt: 'auto',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                  }}
                >
                  <Stack
                    direction="row"
                    spacing={0.75}
                    sx={{ alignItems: 'center' }}
                  >
                    <OdometerWheels
                      size="small"
                      value={units.distance.fromKm(car.currentOdometer.value)}
                      label={t('car.cluster.odometer', {
                        value: format.distance(car.currentOdometer.value),
                      })}
                    />
                    <Typography
                      aria-hidden
                      variant="body2"
                      sx={{ color: 'text.secondary' }}
                    >
                      {units.distance.label}
                    </Typography>
                  </Stack>
                  <Typography
                    variant="body2"
                    className="tabular"
                    sx={{ color: 'text.secondary', whiteSpace: 'nowrap' }}
                  >
                    {t('garage.entries', { count: car.amountBills ?? 0 })}
                  </Typography>
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
                minHeight: 196,
                borderStyle: 'dashed',
                bgcolor: 'transparent',
              }}
              // dark cards have no border: this one keeps its dashes
              style={{ borderColor: 'var(--mui-palette-divider)' }}
            >
              {canAdd ? (
                <RouterCardActionArea
                  to="/car/new"
                  sx={{
                    height: '100%',
                    display: 'grid',
                    placeItems: 'center',
                    p: 2.5,
                  }}
                >
                  <Stack spacing={1.5} sx={{ alignItems: 'center' }}>
                    <EmptyPlate />
                    <Typography sx={{ fontWeight: 600 }}>
                      {t('garage.add')}
                    </Typography>
                    {used === 0 && (
                      <Typography
                        variant="body2"
                        sx={{ color: 'text.secondary' }}
                      >
                        {t('garage.emptyHint')}
                      </Typography>
                    )}
                  </Stack>
                </RouterCardActionArea>
              ) : (
                <Stack
                  spacing={1.5}
                  sx={{
                    alignItems: 'center',
                    justifyContent: 'center',
                    height: '100%',
                    p: 3,
                  }}
                >
                  <EmptyPlate disabled />
                  <Typography
                    variant="body2"
                    sx={{ color: 'text.secondary', textAlign: 'center' }}
                  >
                    {t('garage.limitReached')}
                  </Typography>
                  {carRequest.isOpen ? (
                    <Chip
                      color="info"
                      variant="outlined"
                      label={t('carRequest.pending', {
                        count: carRequest.request?.cars ?? 0,
                      })}
                    />
                  ) : (
                    !carRequest.isLoading && (
                      <Button
                        variant="outlined"
                        size="small"
                        onClick={() => setRequesting(true)}
                      >
                        {t('carRequest.button')}
                      </Button>
                    )
                  )}
                </Stack>
              )}
            </Card>
          </Grid>
        )}
      </Grid>
      <CarRequestDialog
        open={requesting}
        maxCars={maxCars}
        onClose={() => setRequesting(false)}
      />
    </Page>
  );
}
