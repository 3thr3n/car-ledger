import {
  Box,
  Container,
  Skeleton,
  Stack,
  Tabs,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import UploadFileRounded from '@mui/icons-material/UploadFileRounded';
import EditRounded from '@mui/icons-material/EditRounded';
import ArrowBackRounded from '@mui/icons-material/ArrowBackRounded';
import { useQuery } from '@tanstack/react-query';
import { Outlet, useLocation } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import { BillType } from '@/generated';
import { billTypes } from '@/utils/billTypes';
import CarAvatar from '@/components/layout/CarAvatar';
import OdometerValue from '@/components/ui/OdometerValue';
import {
  RouterButton,
  RouterIconButton,
  RouterTab,
} from '@/components/ui/links';
import NotFoundPage from '@/pages/NotFoundPage';
import ErrorPage from '@/pages/ErrorPage';

const tabs = [
  { key: 'overview', to: '/car/$id' },
  ...[
    BillType.FUEL,
    BillType.MAINTENANCE,
    BillType.MISCELLANEOUS,
    BillType.RECURRING,
  ].map((type) => ({ key: type, to: billTypes[type].listRoute })),
  { key: 'reminders', to: '/car/$id/reminders' },
] as const;

export default function CarLayout({ id }: { id: number }) {
  const { t } = useTranslation();
  const { pathname } = useLocation();

  const {
    data: car,
    isLoading,
    isError,
    error,
  } = useQuery(getMyCarOptions({ client: localClient, path: { id } }));

  if (isError) {
    if (error instanceof BackendError && error.status === 404) {
      return <NotFoundPage />;
    }
    return <ErrorPage error={error} />;
  }

  const base = `/car/${id}`;
  const rest = pathname.replace(base, '').replace(/\/$/, '');
  const activeTab =
    rest === ''
      ? 'overview'
      : rest === '/reminders'
        ? 'reminders'
        : (Object.values(BillType).find(
            (type) => rest === `/bill/${type.toLowerCase()}`,
          ) ?? false);
  const isSubPage = activeTab === false;

  // preselect the type of the current tab when adding an entry
  const addType =
    activeTab !== false && activeTab !== 'overview' && activeTab !== 'reminders'
      ? activeTab
      : undefined;
  const importType =
    addType && billTypes[addType].importable ? addType : undefined;
  const params = { id: `${id}` };

  return (
    <Box sx={{ flexGrow: 1, py: { xs: 3, md: 5 } }}>
      <Container maxWidth="lg">
        {isSubPage ? (
          <RouterButton
            to="/car/$id"
            params={params}
            startIcon={<ArrowBackRounded />}
            color="inherit"
            size="small"
            sx={{ mb: 2, ml: -1, color: 'text.secondary' }}
          >
            {car?.name ?? t('nav.garage')}
          </RouterButton>
        ) : (
          <>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              sx={{
                alignItems: { xs: 'flex-start', sm: 'center' },
                mb: 2.5,
              }}
            >
              <Stack
                direction="row"
                spacing={2}
                sx={{
                  alignItems: 'center',
                  flexGrow: 1,
                  minWidth: 0,
                }}
              >
                <CarAvatar
                  id={car?.id}
                  name={car?.name}
                  pictureId={car?.pictureId}
                  size={52}
                />
                <Box sx={{ minWidth: 0 }}>
                  <Typography variant="h4" component="h1" noWrap>
                    {isLoading ? <Skeleton width={220} /> : car?.name}
                  </Typography>
                  <Typography
                    sx={{
                      color: 'text.secondary',
                    }}
                  >
                    {isLoading ? (
                      <Skeleton width={160} />
                    ) : (
                      <>
                        {car?.licensePlate && `${car.licensePlate} · `}
                        {car?.year &&
                          `${t('car.built', { year: car.year })} · `}
                        <OdometerValue odometer={car?.currentOdometer} />
                      </>
                    )}
                  </Typography>
                </Box>
              </Stack>
              <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
                <RouterIconButton
                  to="/car/$id/edit"
                  params={params}
                  aria-label={t('button.edit')}
                  title={t('button.edit')}
                >
                  <EditRounded fontSize="small" />
                </RouterIconButton>
                <RouterButton
                  to="/car/$id/bill/import"
                  params={params}
                  search={{ type: importType }}
                  variant="outlined"
                  startIcon={<UploadFileRounded />}
                >
                  {t('button.import')}
                </RouterButton>
                <RouterButton
                  to="/car/$id/bill/add"
                  params={params}
                  search={{ type: addType }}
                  variant="contained"
                  startIcon={<AddRounded />}
                >
                  {t('bill.add.button')}
                </RouterButton>
              </Stack>
            </Stack>

            <Tabs
              value={activeTab}
              variant="scrollable"
              allowScrollButtonsMobile
              sx={{ mb: 3, borderBottom: 1, borderColor: 'divider' }}
            >
              {tabs.map((tab) => (
                <RouterTab
                  key={tab.key}
                  value={tab.key}
                  label={
                    tab.key === 'overview' || tab.key === 'reminders'
                      ? t(`car.tabs.${tab.key}`)
                      : t(`bill.types.${tab.key}`)
                  }
                  to={tab.to}
                  params={params}
                />
              ))}
            </Tabs>
          </>
        )}

        <Outlet />
      </Container>
    </Box>
  );
}
