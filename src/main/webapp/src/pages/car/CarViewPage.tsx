import { NavigateOptions } from '@tanstack/router-core';
import {
  Card,
  CardContent,
  CircularProgress,
  Container,
  Divider,
  Grid,
  IconButton,
  Stack,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import CarLedgerSubPageHeader from '@/components/CarLedgerSubPageHeader';
import NotFoundPage from '@/pages/NotFoundPage';
import useCsvStore from '@/store/CsvStore';
import { CarOverviewStats } from '@/components/car/CarOverviewStats';
import SingleLineStat from '@/components/base/SingleLineStat';
import CarLedgerPageHeader from '@/components/CarLedgerPageHeader';
import { useTranslation } from 'react-i18next';
import RecentRefuels from '@/components/car/bill/fuel/RecentRefuels';
import CarLedgerButton from '@/components/CarLedgerButton';
import { Edit } from '@mui/icons-material';
import CarLedgerPage from '@/components/CarLedgerPage';
import RecentMaintenance from '@/components/car/bill/maintenance/RecentMaintenance';
import RecentMiscellaneous from '@/components/car/bill/miscellaneous/RecentMiscellaneous';

export interface CarViewPageProperties {
  id: string;
  navigate: (opt: NavigateOptions) => void;
}

export default function CarViewPage({ navigate, id }: CarViewPageProperties) {
  const { t } = useTranslation();
  const isMobile = useMediaQuery('(max-width:600px)');
  const openImportDialog = useCsvStore((state) => state.openDialog);
  const csvImportedAt = useCsvStore((state) => state.csvImportedAt);

  const {
    data: car,
    isLoading: isCarLoading,
    isError: isCarError,
  } = useQuery({
    ...getMyCarOptions({
      path: {
        id: Number(id),
      },
      client: localClient,
    }),
  });

  if (isCarLoading) {
    return (
      <Container sx={{ mt: 8 }}>
        <CircularProgress />
        <Typography variant="body1">Loading car details...</Typography>
      </Container>
    );
  }

  if (isCarError || !car) {
    return (
      <Container sx={{ mt: 8 }}>
        <Typography variant="body1" color="error">
          Could not load car details.
        </Typography>
      </Container>
    );
  }

  if (!car.id) {
    return <NotFoundPage />;
  }

  return (
    <CarLedgerPage id="CarViewPage">
      <Container>
        {/* Header */}
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          justifyContent="space-between"
          alignItems={{ xs: 'flex-start', sm: 'center' }}
          spacing={2}
          mb={3}
        >
          <CarLedgerPageHeader
            title={`${car.name} ${car.year && '(' + car.year + ')'}`}
          >
            <IconButton
              sx={{
                ml: 1,
              }}
              onClick={() => navigate({ to: '/car/$id/edit', params: { id } })}
            >
              <Edit />
            </IconButton>
          </CarLedgerPageHeader>

          <Stack direction="row" spacing={2}>
            <CarLedgerButton
              variant="contained"
              onClick={() =>
                navigate({ to: '/car/$id/bill/add', params: { id } })
              }
            >
              {t('app.car.add')}
            </CarLedgerButton>
            <CarLedgerButton
              variant="outlined"
              color="secondary"
              onClick={() => openImportDialog(Number(id))}
            >
              {t('app.car.csvImport')}
            </CarLedgerButton>
          </Stack>
        </Stack>

        <Grid container spacing={3}>
          {/* Car info summary */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Card>
              <CardContent>
                <CarLedgerSubPageHeader
                  title={t('app.car.info')}
                  isCardHeader
                />
                <Divider sx={{ mb: 2 }} />
                <SingleLineStat
                  label={t('app.car.common.year') + ':'}
                  value={car.year}
                />
                <SingleLineStat
                  label={t('app.car.common.odometer') + ':'}
                  value={car.odometer}
                  type="km"
                />
              </CardContent>
            </Card>
            <CarOverviewStats
              carId={Number(id)}
              reload={csvImportedAt ?? 0}
              navigate={navigate}
            />
          </Grid>
          <Grid container size={{ xs: 12, md: 8 }}>
            <RecentRefuels
              isMobile={isMobile}
              id={id}
              navigate={navigate}
              reloadToken={csvImportedAt ?? 0}
            />
            <RecentMaintenance
              isMobile={isMobile}
              id={id}
              navigate={navigate}
              reloadToken={csvImportedAt ?? 0}
            />
            <RecentMiscellaneous
              isMobile={isMobile}
              id={id}
              navigate={navigate}
              reloadToken={csvImportedAt ?? 0}
            />
          </Grid>
        </Grid>
      </Container>
    </CarLedgerPage>
  );
}
