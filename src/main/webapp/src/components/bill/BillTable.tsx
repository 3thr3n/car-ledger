import {
  Box,
  ButtonBase,
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
import EditOutlined from '@mui/icons-material/EditOutlined';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import { DataGrid, GridColDef, GridValidRowModel } from '@mui/x-data-grid';
import { deDE } from '@mui/x-data-grid/locales';
import { Fragment, ReactNode, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import AttachmentsDialog from './AttachmentsDialog';
import CreatorHint from '@/components/ui/CreatorHint';
import { previewUrl } from './attachments';
import EmptyState from '@/components/ui/EmptyState';
import { PageModel, SortModel } from '@/hooks/useBillList';

export interface MobileRow {
  primary: ReactNode;
  secondary?: ReactNode;
  amount?: ReactNode;
  badge?: ReactNode;
}

export interface BillTableProps<
  R extends GridValidRowModel & {
    id?: number;
    attachments?: number;
    photo?: number;
    /** `null` once that account was deleted */
    createdBy?: string | null;
  },
> {
  carId: number;
  /** whether the rows are recurring costs, for their files */
  recurring?: boolean;
  rows: R[];
  total: number;
  columns: GridColDef<R>[];
  pageModel: PageModel;
  onPageModelChange: (model: PageModel) => void;
  /** sorting on the server, without it the columns cannot be sorted */
  sortModel?: SortModel;
  onSortModelChange?: (model: SortModel | undefined) => void;
  loading?: boolean;
  onDelete?: (id: number) => void;
  /** opens the edit form of a row */
  onEdit?: (row: R) => void;
  /** further actions of a row, shown before edit and delete */
  rowActions?: (row: R) => ReactNode;
  mobileRow: (row: R) => MobileRow;
  emptyTitle: ReactNode;
  emptyText?: ReactNode;
  emptyAction?: ReactNode;
}

export default function BillTable<
  R extends GridValidRowModel & {
    id?: number;
    attachments?: number;
    photo?: number;
    /** `null` once that account was deleted */
    createdBy?: string | null;
  },
>({
  carId,
  recurring,
  rows,
  total,
  columns,
  pageModel,
  onPageModelChange,
  sortModel,
  onSortModelChange,
  loading,
  onDelete,
  onEdit,
  rowActions,
  mobileRow,
  emptyTitle,
  emptyText,
  emptyAction,
}: BillTableProps<R>) {
  const { t, i18n } = useTranslation();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const [toDelete, setToDelete] = useState<number>();
  const [shown, setShown] = useState<R>();
  // the grid compares the sort model by reference: a new array on every render
  // counts as a new order and sends the grid back to the first page
  const gridSortModel = useMemo(
    () => (sortModel ? [sortModel] : []),
    [sortModel],
  );

  const dialogs = (
    <>
      <ConfirmDialog
        open={toDelete !== undefined}
        title={t('bill.delete.title')}
        text={t('bill.delete.text')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() => toDelete !== undefined && onDelete?.(toDelete)}
      />
      <AttachmentsDialog
        carId={carId}
        billId={shown?.id}
        recurring={recurring}
        onClose={() => setShown(undefined)}
        onEdit={shown && onEdit && (() => onEdit(shown))}
      />
    </>
  );

  const hint = (row: R) => (
    <>
      <AttachmentHint
        count={row.attachments}
        photo={row.photo ? previewUrl(carId, row.photo) : undefined}
        onClick={() => setShown(row)}
      />
      <CreatorHint name={row.createdBy} />
    </>
  );

  const actions = (row: R) => (
    <Stack
      direction="row"
      spacing={0.25}
      sx={{
        alignItems: 'center',
        justifyContent: 'flex-end',
        height: '100%',
      }}
    >
      {rowActions?.(row)}
      {onEdit && (
        <Tooltip title={t('button.edit')}>
          <IconButton
            size="small"
            onClick={(event) => {
              event.stopPropagation();
              onEdit(row);
            }}
          >
            <EditOutlined fontSize="small" />
          </IconButton>
        </Tooltip>
      )}
      {deleteButton(row.id)}
    </Stack>
  );
  const actionCount =
    (onDelete ? 1 : 0) + (onEdit ? 1 : 0) + (rowActions ? 1 : 0);

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
                sx={{
                  alignItems: 'center',
                  px: 2,
                  py: 1.5,
                }}
              >
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography
                    noWrap
                    sx={{
                      fontWeight: 600,
                    }}
                  >
                    {item.primary}
                    {hint(row)}
                  </Typography>
                  <Stack
                    direction="row"
                    spacing={1}
                    sx={{
                      alignItems: 'center',
                    }}
                  >
                    {item.badge}
                    {item.secondary && (
                      <Typography
                        variant="body2"
                        noWrap
                        sx={{
                          color: 'text.secondary',
                        }}
                      >
                        {item.secondary}
                      </Typography>
                    )}
                  </Stack>
                </Box>
                {item.amount && (
                  <Typography
                    className="tabular"
                    sx={{
                      fontWeight: 650,
                      flexShrink: 0,
                    }}
                  >
                    {item.amount}
                  </Typography>
                )}
                {actions(row)}
              </Stack>
            </Fragment>
          );
        })}
        {pages > 1 && (
          <Stack
            sx={{
              alignItems: 'center',
              py: 2,
            }}
          >
            <Pagination
              count={pages}
              page={pageModel.page + 1}
              onChange={(_, page) =>
                onPageModelChange({ ...pageModel, page: page - 1 })
              }
            />
          </Stack>
        )}
        {dialogs}
      </Box>
    );
  }

  // the date (the name of a recurring cost) shows whether files are attached
  const withHint = columns.map((column) =>
    column.field === (recurring ? 'name' : 'date')
      ? {
          ...column,
          renderCell: ({ formattedValue, row }) => (
            <>
              {formattedValue}
              {hint(row)}
            </>
          ),
        }
      : column,
  ) as GridColDef<R>[];

  const allColumns: GridColDef<R>[] =
    actionCount > 0
      ? [
          ...withHint,
          {
            field: '__actions',
            headerName: '',
            sortable: false,
            filterable: false,
            disableColumnMenu: true,
            width: 16 + actionCount * 38,
            align: 'right',
            renderCell: ({ row }) => actions(row),
          },
        ]
      : withHint;

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
        disableColumnSorting={!onSortModelChange}
        sortingMode="server"
        sortModel={gridSortModel}
        onSortModelChange={(model) => {
          const [first] = model;
          onSortModelChange?.(
            first?.sort ? { field: first.field, sort: first.sort } : undefined,
          );
        }}
        autoHeight
        density="comfortable"
        localeText={
          i18n.language === 'DE'
            ? deDE.components.MuiDataGrid.defaultProps.localeText
            : undefined
        }
      />
      {dialogs}
    </>
  );
}

/**
 * the first photo small (a paperclip without photos) with the amount of
 * attached files that shows them, nothing without
 */
function AttachmentHint({
  count,
  photo,
  onClick,
}: {
  count?: number;
  /** address of the preview of the first photo */
  photo?: string;
  onClick: () => void;
}) {
  const { t } = useTranslation();
  if (!count) return null;
  return (
    <Tooltip title={t('attachments.count', { count })}>
      <ButtonBase
        aria-label={t('attachments.count', { count })}
        onClick={(event) => {
          event.stopPropagation();
          onClick();
        }}
        sx={{
          display: 'inline-flex',
          alignItems: 'center',
          verticalAlign: 'middle',
          ml: 0.75,
          px: 0.5,
          py: 0.25,
          borderRadius: 1,
          color: 'text.secondary',
          fontSize: '0.75rem',
          fontWeight: 500,
          '&:hover': { color: 'primary.main', bgcolor: 'action.hover' },
        }}
      >
        {photo ? (
          <Box
            component="img"
            src={photo}
            alt=""
            loading="lazy"
            sx={{
              width: 20,
              height: 20,
              mr: 0.25,
              borderRadius: 0.5,
              objectFit: 'cover',
              border: 1,
              borderColor: 'divider',
            }}
          />
        ) : (
          <AttachFileRounded
            sx={{ fontSize: 16, transform: 'rotate(45deg)' }}
          />
        )}
        {count > 1 && count}
      </ButtonBase>
    </Tooltip>
  );
}
