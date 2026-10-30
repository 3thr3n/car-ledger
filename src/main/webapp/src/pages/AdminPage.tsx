import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import SearchRounded from '@mui/icons-material/SearchRounded';
import EditOutlined from '@mui/icons-material/EditOutlined';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import PeopleRounded from '@mui/icons-material/PeopleRounded';
import HowToRegRounded from '@mui/icons-material/HowToRegRounded';
import DirectionsCarRounded from '@mui/icons-material/DirectionsCarRounded';
import ReceiptLongRounded from '@mui/icons-material/ReceiptLongRounded';
import EventRepeatRounded from '@mui/icons-material/EventRepeatRounded';
import LockRounded from '@mui/icons-material/LockRounded';
import MarkEmailUnreadRounded from '@mui/icons-material/MarkEmailUnreadRounded';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import { DataGrid, GridColDef } from '@mui/x-data-grid';
import { deDE } from '@mui/x-data-grid/locales';
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import { useDeferredValue, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { AdminAccount } from '@/generated';
import {
  deleteAdminAccountMutation,
  getAdminAccountsOptions,
  getAdminStatsOptions,
  updateAdminAccountMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { fileSize } from '@/utils/format';
import useMe from '@/hooks/useMe';
import { PageModel } from '@/hooks/useBillList';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import StatTile from '@/components/ui/StatTile';
import EmptyState from '@/components/ui/EmptyState';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import NumberField from '@/components/bill/NumberField';
import CarRequestList from '@/components/admin/CarRequestList';
import { visuallyHidden } from '@/components/ui/visuallyHidden';

/** refreshes everything shown on the page, and the own account */
const refresh = () =>
  queryClient.invalidateQueries({
    predicate: (query) => {
      const id = (query.queryKey[0] as { _id?: string } | undefined)?._id ?? '';
      return (
        id.startsWith('getAdmin') || id === 'getMyself' || id === 'getMyCars'
      );
    },
  });

const showError = (fallback: string) => (error: Error) =>
  toast.error(errorMessage(error, fallback));

export default function AdminPage() {
  const { t } = useTranslation();
  const { isAdmin, isLoading } = useMe();

  if (isLoading) return null;
  if (!isAdmin) {
    return (
      <Page id="AdminPage">
        <EmptyState
          icon={<LockRounded />}
          title={t('admin.forbidden.title')}
          text={t('admin.forbidden.text')}
        />
      </Page>
    );
  }

  return (
    <Page id="AdminPage">
      <PageHeader title={t('admin.title')} subtitle={t('admin.subtitle')} />
      <AdminStats />
      <CarRequestList />
      <AccountTable />
    </Page>
  );
}

function AdminStats() {
  const { t, i18n } = useTranslation();
  const { data, isLoading } = useQuery(
    getAdminStatsOptions({ client: localClient }),
  );

  const tiles = [
    { key: 'accounts', icon: <PeopleRounded />, value: data?.accounts },
    {
      key: 'activeAccounts',
      icon: <HowToRegRounded />,
      value: data?.activeAccounts,
    },
    { key: 'cars', icon: <DirectionsCarRounded />, value: data?.cars },
    { key: 'bills', icon: <ReceiptLongRounded />, value: data?.bills },
    {
      key: 'recurringBills',
      icon: <EventRepeatRounded />,
      value: data?.recurringBills,
    },
    {
      key: 'openRequests',
      icon: <MarkEmailUnreadRounded />,
      value: data?.openRequests,
    },
    {
      key: 'files',
      icon: <AttachFileRounded />,
      value: data?.files.reduce((sum, storage) => sum + storage.files, 0),
      // per storage, e.g. "Database: 120 (300 MB) · S3: 5 (2 MB)"
      hint:
        data &&
        [
          ...data.files.map(
            (storage) =>
              `${t(`admin.stats.storage.${storage.storage}`, storage.storage)}: ${storage.files} (${fileSize(i18n.language, storage.bytes)})`,
          ),
          t('admin.stats.newFiles', {
            storage: t(
              `admin.stats.storage.${data.fileStorage}`,
              data.fileStorage,
            ),
          }),
        ].join(' · '),
    },
  ];

  return (
    <Grid container spacing={2} sx={{ mb: 3 }}>
      {tiles.map((tile) => (
        <Grid key={tile.key} size={{ xs: 6, sm: 4, lg: 'grow' }}>
          <StatTile
            loading={isLoading}
            icon={tile.icon}
            label={t(`admin.stats.${tile.key}`)}
            value={tile.value ?? 0}
            hint={tile.hint}
          />
        </Grid>
      ))}
    </Grid>
  );
}

function AccountTable() {
  const { t, i18n } = useTranslation();
  const { account: me } = useMe();
  const theme = useTheme();
  const isPhone = useMediaQuery(theme.breakpoints.down('sm'));
  const [search, setSearch] = useState('');
  const deferredSearch = useDeferredValue(search.trim());
  const [pageModel, setPageModel] = useState<PageModel>({
    page: 0,
    pageSize: 20,
  });
  const [toEdit, setToEdit] = useState<AdminAccount>();
  const [toDelete, setToDelete] = useState<AdminAccount>();

  const { data, isLoading, isFetching } = useQuery({
    ...getAdminAccountsOptions({
      client: localClient,
      query: {
        page: pageModel.page + 1,
        size: pageModel.pageSize,
        search: deferredSearch || undefined,
      },
    }),
    placeholderData: keepPreviousData,
  });

  const remove = useMutation({
    ...deleteAdminAccountMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('admin.delete.done'));
      return refresh();
    },
    onError: showError(t('errors.backend')),
  });

  const date = (value?: string) =>
    value
      ? new Intl.DateTimeFormat(i18n.language === 'DE' ? 'de-DE' : 'en-US', {
          dateStyle: 'medium',
        }).format(new Date(value))
      : t('admin.never');

  const columns: GridColDef<AdminAccount>[] = [
    {
      field: 'name',
      headerName: t('admin.columns.name'),
      flex: 1,
      minWidth: isPhone ? 110 : 180,
      renderCell: ({ row }) => (
        <Stack
          direction="row"
          spacing={1}
          sx={{
            alignItems: 'center',
            height: '100%',
          }}
        >
          <span>{row.name}</span>
          {row.name === me?.name && (
            <Chip size="small" label={t('admin.you')} />
          )}
        </Stack>
      ),
    },
    {
      field: 'cars',
      headerName: t('admin.columns.cars'),
      width: isPhone ? 72 : 110,
      align: 'right',
      headerAlign: 'right',
      valueGetter: (_, row) => `${row.cars ?? 0} / ${row.maxCars ?? 0}`,
    },
    {
      field: 'bills',
      headerName: t('admin.columns.bills'),
      width: 110,
      align: 'right',
      headerAlign: 'right',
    },
    {
      field: 'createdAt',
      headerName: t('admin.columns.createdAt'),
      width: 140,
      valueFormatter: (value: string) => date(value),
    },
    {
      field: 'lastSeen',
      headerName: t('admin.columns.lastSeen'),
      width: 140,
      valueFormatter: (value: string) => date(value),
    },
    {
      field: '__actions',
      // named for screen readers, the buttons explain themselves
      headerName: t('common.actions'),
      renderHeader: () => (
        <Box component="span" sx={visuallyHidden}>
          {t('common.actions')}
        </Box>
      ),
      width: 96,
      sortable: false,
      disableColumnMenu: true,
      align: 'right',
      renderCell: ({ row }) => (
        <Stack
          direction="row"
          sx={{
            alignItems: 'center',
            justifyContent: 'flex-end',
            height: '100%',
          }}
        >
          <Tooltip title={t('admin.edit.button')}>
            <IconButton size="small" onClick={() => setToEdit(row)}>
              <EditOutlined fontSize="small" />
            </IconButton>
          </Tooltip>
          <Tooltip
            title={
              row.name === me?.name
                ? t('admin.delete.self')
                : t('button.delete')
            }
          >
            {/* span, so the tooltip works on the disabled button */}
            <span>
              <IconButton
                size="small"
                disabled={row.name === me?.name}
                onClick={() => setToDelete(row)}
                aria-label={t('button.delete')}
              >
                <DeleteOutlineRounded fontSize="small" />
              </IconButton>
            </span>
          </Tooltip>
        </Stack>
      ),
    },
  ];

  return (
    <Box>
      <TextField
        size="small"
        placeholder={t('admin.search')}
        value={search}
        onChange={(e) => {
          setSearch(e.target.value);
          setPageModel((model) => ({ ...model, page: 0 }));
        }}
        sx={{ mb: 2, maxWidth: 360 }}
        slotProps={{
          input: {
            startAdornment: (
              <InputAdornment position="start">
                <SearchRounded fontSize="small" />
              </InputAdornment>
            ),
          },
        }}
      />
      <Box
        sx={{
          bgcolor: 'background.paper',
          borderRadius: 3.5,
          border: 1,
          borderColor: 'divider',
        }}
      >
        <DataGrid
          rows={data?.data ?? []}
          columns={columns}
          getRowId={(row) => row.id!}
          rowCount={data?.total ?? 0}
          loading={isLoading || isFetching}
          paginationMode="server"
          paginationModel={pageModel}
          onPaginationModelChange={setPageModel}
          pageSizeOptions={[10, 20, 50, 100]}
          disableRowSelectionOnClick
          disableColumnFilter
          disableColumnSorting
          autoHeight
          columnVisibilityModel={
            isPhone ? { bills: false, createdAt: false, lastSeen: false } : {}
          }
          localeText={
            i18n.language === 'DE'
              ? deDE.components.MuiDataGrid.defaultProps.localeText
              : undefined
          }
        />
      </Box>

      <MaxCarsDialog account={toEdit} onClose={() => setToEdit(undefined)} />
      <ConfirmDialog
        open={toDelete !== undefined}
        title={t('admin.delete.title', { name: toDelete?.name })}
        text={t('admin.delete.text', {
          cars: toDelete?.cars ?? 0,
          bills: toDelete?.bills ?? 0,
        })}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete?.id !== undefined &&
          remove.mutate({ path: { id: toDelete.id } })
        }
      />
    </Box>
  );
}

function MaxCarsDialog({
  account,
  onClose,
}: {
  account?: AdminAccount;
  onClose: () => void;
}) {
  return (
    <Dialog
      open={account !== undefined}
      onClose={onClose}
      maxWidth="xs"
      fullWidth
    >
      {/* keyed, so the field starts with the value of the selected account */}
      {account && (
        <MaxCarsForm key={account.id} account={account} onClose={onClose} />
      )}
    </Dialog>
  );
}

function MaxCarsForm({
  account,
  onClose,
}: {
  account: AdminAccount;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [maxCars, setMaxCars] = useState<number | undefined>(account.maxCars);

  const update = useMutation({
    ...updateAdminAccountMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('admin.edit.done'));
      onClose();
      return refresh();
    },
    onError: showError(t('errors.backend')),
  });

  const invalid = maxCars === undefined || maxCars < 0 || maxCars > 100;

  return (
    <>
      <DialogTitle>{t('admin.edit.title', { name: account.name })}</DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <NumberField
            label={t('admin.edit.maxCars')}
            value={maxCars}
            onChange={setMaxCars}
            decimalScale={0}
            max={100}
            helperText={t('admin.edit.help', { cars: account.cars ?? 0 })}
          />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={invalid || update.isPending}
          onClick={() =>
            account.id !== undefined &&
            update.mutate({ path: { id: account.id }, body: { maxCars } })
          }
        >
          {t('button.save')}
        </Button>
      </DialogActions>
    </>
  );
}
