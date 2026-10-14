import { Box, Button, Stack } from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import { GridColDef, GridValidRowModel } from '@mui/x-data-grid';
import { useNavigate } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import useBillList from '@/hooks/useBillList';
import Section from '@/components/ui/Section';
import YearFilter from '@/components/ui/YearFilter';
import { RouterButton } from '@/components/ui/links';
import BillTable, { MobileRow } from '@/components/bill/BillTable';
import {
  useFuelColumns,
  useMaintenanceColumns,
  useMiscellaneousColumns,
} from '@/components/bill/columns';
import useDeleteBill from '@/components/bill/useDeleteBill';
import { baseUrl } from '@/utils/QueryClient';

type ListType = BillType.FUEL | BillType.MAINTENANCE | BillType.MISCELLANEOUS;

const kinds = {
  [BillType.FUEL]: 'fuel',
  [BillType.MAINTENANCE]: 'maintenance',
  [BillType.MISCELLANEOUS]: 'miscellaneous',
} as const;

/** the bills of the list as CSV, in the format of the import */
function exportUrl(carId: number, type: ListType, year?: number) {
  const query = new URLSearchParams({ type });
  if (year !== undefined) query.set('year', `${year}`);
  return `${baseUrl}/api/export/${carId}?${query}`;
}

function useColumns(type: ListType) {
  const fuel = useFuelColumns();
  const maintenance = useMaintenanceColumns();
  const miscellaneous = useMiscellaneousColumns();
  const all = {
    [BillType.FUEL]: fuel,
    [BillType.MAINTENANCE]: maintenance,
    [BillType.MISCELLANEOUS]: miscellaneous,
  };
  // the row type matches the list kind of the same bill type
  return all[type] as unknown as {
    columns: GridColDef<GridValidRowModel>[];
    mobileRow: (row: GridValidRowModel) => MobileRow;
  };
}

export default function BillListPage({
  carId,
  type,
}: {
  carId: number;
  type: ListType;
}) {
  const { t } = useTranslation();
  const list = useBillList(kinds[type], carId);
  const { columns, mobileRow } = useColumns(type);
  const onDelete = useDeleteBill(carId);
  const navigate = useNavigate();

  return (
    <Section flush>
      <Stack
        direction="row"
        spacing={1.5}
        sx={{ px: 2.5, pt: 2, pb: 1.5, alignItems: 'center' }}
      >
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <YearFilter
            years={list.years}
            value={list.year}
            onChange={list.setYear}
          />
        </Box>
        {list.total > 0 && (
          <Button
            href={exportUrl(carId, type, list.year)}
            variant="outlined"
            size="small"
            startIcon={<FileDownloadRounded />}
            sx={{ flexShrink: 0 }}
          >
            {t('button.export')}
          </Button>
        )}
      </Stack>
      <BillTable
        carId={carId}
        rows={list.rows}
        total={list.total}
        columns={columns}
        mobileRow={mobileRow}
        pageModel={list.pageModel}
        onPageModelChange={list.setPageModel}
        sortModel={list.sortModel}
        onSortModelChange={list.setSortModel}
        loading={list.isLoading || list.isFetching}
        onDelete={onDelete}
        onEdit={(row) =>
          navigate({
            to: '/car/$id/bill/$billId/edit',
            params: { id: `${carId}`, billId: `${row.id}` },
            search: { type },
          })
        }
        emptyTitle={t('bill.empty.title', { type: t(`bill.types.${type}`) })}
        emptyText={t(`bill.empty.${type}`)}
        emptyAction={
          <RouterButton
            to="/car/$id/bill/add"
            params={{ id: `${carId}` }}
            search={{ type }}
            variant="contained"
          >
            {t('bill.add.button')}
          </RouterButton>
        }
      />
    </Section>
  );
}
