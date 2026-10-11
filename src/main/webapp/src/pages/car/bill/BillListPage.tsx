import { Box } from '@mui/material';
import { GridColDef, GridValidRowModel } from '@mui/x-data-grid';
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

type ListType = BillType.FUEL | BillType.MAINTENANCE | BillType.MISCELLANEOUS;

const kinds = {
  [BillType.FUEL]: 'fuel',
  [BillType.MAINTENANCE]: 'maintenance',
  [BillType.MISCELLANEOUS]: 'miscellaneous',
} as const;

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

  return (
    <Section flush>
      <Box sx={{ px: 2.5, pt: 2, pb: 1.5 }}>
        <YearFilter
          years={list.years}
          value={list.year}
          onChange={list.setYear}
        />
      </Box>
      <BillTable
        rows={list.rows}
        total={list.total}
        columns={columns}
        mobileRow={mobileRow}
        pageModel={list.pageModel}
        onPageModelChange={list.setPageModel}
        loading={list.isLoading || list.isFetching}
        onDelete={onDelete}
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
