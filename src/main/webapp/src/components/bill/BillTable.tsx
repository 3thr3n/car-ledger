import {
  Box,
  Divider,
  IconButton,
  LinearProgress,
  Pagination,
  Stack,
  Tooltip,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import { DataGrid, GridColDef, GridValidRowModel } from '@mui/x-data-grid';
import { deDE } from '@mui/x-data-grid/locales';
import { Fragment, ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import EmptyState from '@/components/ui/EmptyState';
import { PageModel } from '@/hooks/useBillList';

export interface MobileRow {
  primary: ReactNode;
  secondary?: ReactNode;
  amount?: ReactNode;
  badge?: ReactNode;
}

export interface BillTableProps<R extends GridValidRowModel & { id?: number }> {
  rows: R[];
  total: number;
  columns: GridColDef<R>[];
  pageModel: PageModel;
  onPageModelChange: (model: PageModel) => void;
  loading?: boolean;
  onDelete?: (id: number) => void;
  mobileRow: (row: R) => MobileRow;
  emptyTitle: ReactNode;
  emptyText?: ReactNode;
  emptyAction?: ReactNode;
}

export default function BillTable<
  R extends GridValidRowModel & { id?: number },
>({
  rows,
  total,
  columns,
  pageModel,
  onPageModelChange,
  loading,
  onDelete,
  mobileRow,
  emptyTitle,
  emptyText,
  emptyAction,
}: BillTableProps<R>) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [toDelete, setToDelete] = useState<number>();

  const confirm = (
    <ConfirmDialog
      open={toDelete !== undefined}
      title={t('bill.delete.title')}
      text={t('bill.delete.text')}
      onClose={() => setToDelete(undefined)}
      onConfirm={() => toDelete !== undefined && onDelete?.(toDelete)}
    />
  );

  const deleteButton = (id?: number) =>
    onDelete &&
    id !== undefined && (
      <Tooltip title={t('button.delete')}>
        <IconButton
          size="small"
          onClick={(event) => {
            event.stopPropagation();
            setToDelete(id);
          }}
        >
          <DeleteOutlineRounded fontSize="small" />
        </IconButton>
      </Tooltip>
    );

  if (!loading && total === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        text={emptyText}
        action={emptyAction}
        compact
      />
    );
  }

  if (isMobile) {
    const pages = Math.max(1, Math.ceil(total / pageModel.pageSize));
    return (
      <Box>
        {loading && <LinearProgress />}
        {rows.map((row, index) => {
          const item = mobileRow(row);
          return (
            <Fragment key={row.id}>
              {index > 0 && <Divider />}
              <Stack
                direction="row"
                spacing={1.5}
                alignItems="center"
                sx={{ px: 2, py: 1.5 }}
              >
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography fontWeight={600} noWrap>
                    {item.primary}
                  </Typography>
                  <Stack direction="row" spacing={1} alignItems="center">
                    {item.badge}
                    {item.secondary && (
                      <Typography variant="body2" color="text.secondary" noWrap>
                        {item.secondary}
                      </Typography>
                    )}
                  </Stack>
                </Box>
                {item.amount && (
                  <Typography
                    fontWeight={650}
                    className="tabular"
                    sx={{ flexShrink: 0 }}
                  >
                    {item.amount}
                  </Typography>
                )}
                {deleteButton(row.id)}
              </Stack>
            </Fragment>
          );
        })}
        {pages > 1 && (
          <Stack alignItems="center" sx={{ py: 2 }}>
            <Pagination
              count={pages}
              page={pageModel.page + 1}
              onChange={(_, page) =>
                onPageModelChange({ ...pageModel, page: page - 1 })
              }
            />
          </Stack>
        )}
        {confirm}
      </Box>
    );
  }

  const allColumns: GridColDef<R>[] = onDelete
    ? [
        ...columns,
        {
          field: '__actions',
          headerName: '',
          sortable: false,
          filterable: false,
          disableColumnMenu: true,
          width: 56,
          align: 'center',
          renderCell: ({ row }) => deleteButton(row.id),
        },
      ]
    : columns;

  return (
    <>
      <DataGrid
        rows={rows}
        columns={allColumns}
        getRowId={(row) => row.id!}
        rowCount={total}
        loading={loading}
        paginationMode="server"
        paginationModel={pageModel}
        onPaginationModelChange={onPageModelChange}
        pageSizeOptions={[10, 20, 50, 100]}
        disableRowSelectionOnClick
        disableColumnFilter
        disableColumnSorting
        autoHeight
        density="comfortable"
        localeText={
          i18n.language === 'DE'
            ? deDE.components.MuiDataGrid.defaultProps.localeText
            : undefined
        }
      />
      {confirm}
    </>
  );
}
