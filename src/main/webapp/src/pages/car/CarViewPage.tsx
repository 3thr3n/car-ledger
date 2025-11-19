import { NavigateOptions } from '@tanstack/router-core';
import {
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Container,
  Divider,
  Grid,
  Stack,
  Typography,
  useMediaQuery,
} from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import CarBillPreviewTable from '@/components/car/bill/CarBillPreviewTable';
import SubPageHeader from '@/components/base/SubPageHeader';
import NotFoundPage from '@/pages/NotFoundPage';
import useCsvStore from '@/store/CsvStore';
import { CarOverviewStats } from '@/components/car/CarOverviewStats';
import SingleLineStat from '@/components/base/SingleLineStat';
import PageHeader from '@/components/base/PageHeader';

export interface CarViewPageProperties {
  id: string;
  navigate: (opt: NavigateOptions) => void;
}

function renderRecentFuelTypes(
  isMobile: boolean,
  id: string,
  navigate: (opt: NavigateOptions) => void,
) {
  const goToAll = () => navigate({ to: '/car/$id/bill/fuel', params: { id } });

  if (isMobile) {
    return (
      <>
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent>
              <SubPageHeader
                title="Recent Fuel Entries"
                isCardHeader
                isMobile
              />
              <Divider sx={{ mb: 2 }} />
              <Box display="flex" justifyContent="flex-end" mt={2}>
                <Button size="small" onClick={goToAll}>
                  See all entries →
                </Button>
              </Box>
            </CardContent>
          </Card>
        </Grid>
      </>
    );
  }
  return (
    <>
      {/* Fuel bills */}
      <Grid size={{ xs: 12, md: 8 }}>
        <Card>
          <CardContent>
            <SubPageHeader title="Recent Fuel Entries" isCardHeader />
            <Divider sx={{ mb: 2 }} />
            <CarBillPreviewTable id={id} onSeeMore={goToAll} />
          </CardContent>
        </Card>
      </Grid>
    </>
  );
}

export default function CarViewPage({ navigate, id }: CarViewPageProperties) {
  const isMobile = useMediaQuery('(max-width:600px)');
  const openImportDialog = useCsvStore((state) => state.openDialog);

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
    <Container sx={{ py: 4 }}>
      {/* Header */}
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        justifyContent="space-between"
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        spacing={2}
        mb={3}
      >
        <PageHeader
          title={`${car.name} ${car.year && '(' + car.year + ')'}`}
          navigate={navigate}
        />
        <Stack direction="row" spacing={2}>
          <Button
            variant="outlined"
            onClick={() => navigate({ to: '/car/$id/edit', params: { id } })}
          >
            Edit Car
          </Button>
          <Button
            variant="contained"
            onClick={() =>
              navigate({ to: '/car/$id/bill/add', params: { id } })
            }
          >
            Add Bill
          </Button>
          <Button
            variant="contained"
            color="secondary"
            onClick={() => openImportDialog(Number(id))}
          >
            Import CSV
          </Button>
        </Stack>
      </Stack>

      <Grid container spacing={3}>
        {/* Car info summary */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <SubPageHeader title="Car Information" isCardHeader />
              <Divider sx={{ mb: 2 }} />
              <SingleLineStat label="Year:" value={car.year} />
              <SingleLineStat
                label="Odometer:"
                value={car.odometer}
                type="km"
              />
            </CardContent>
          </Card>
          <CarOverviewStats carId={Number(id)} />
        </Grid>

        {renderRecentFuelTypes(isMobile, id, navigate)}
      </Grid>
    </Container>
  );
}
