import {
  FormControlLabel,
  IconButton,
  Stack,
  Switch,
  Tooltip,
  Typography,
} from '@mui/material';
import EventBusyRounded from '@mui/icons-material/EventBusyRounded';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType, RecurringBill } from '@/generated';
import { getAllRecurringBillsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { PageModel } from '@/hooks/useBillList';
import Section from '@/components/ui/Section';
import { RouterButton } from '@/components/ui/links';
import BillTable from '@/components/bill/BillTable';
import { useRecurringColumns } from '@/components/bill/columns';
import useDeleteBill from '@/components/bill/useDeleteBill';
import EndRecurringDialog from '@/components/bill/EndRecurringDialog';

export default function RecurringListPage({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const [onlyRunning, setOnlyRunning] = useState(false);
  const [pageModel, setPageModel] = useState<PageModel>({
    page: 0,
    pageSize: 20,
  });
  const { columns, mobileRow } = useRecurringColumns();
  const onDelete = useDeleteBill(carId, true);
  const [toEnd, setToEnd] = useState<RecurringBill>();

  const { data, isLoading, isFetching } = useQuery({
    ...getAllRecurringBillsOptions({
      client: localClient,
      path: { carId },
      query: {
        onlyRunning,
        page: pageModel.page + 1,
        size: pageModel.pageSize,
      },
    }),
    placeholderData: keepPreviousData,
  });

  return (
    <Section flush>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={1}
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
        sx={{ px: 2.5, pt: 1.5, pb: 1 }}
      >
        <Typography variant="body2" color="text.secondary">
          {t('bill.recurringInfo')}
        </Typography>
        <FormControlLabel
          control={
            <Switch
              checked={onlyRunning}
              onChange={(e) => {
                setOnlyRunning(e.target.checked);
                setPageModel((model) => ({ ...model, page: 0 }));
              }}
            />
          }
          label={t('bill.onlyRunning')}
          sx={{ flexShrink: 0 }}
        />
      </Stack>
      <BillTable
        rows={data?.data ?? []}
        total={data?.total ?? 0}
        columns={columns}
        mobileRow={mobileRow}
        pageModel={pageModel}
        onPageModelChange={setPageModel}
        loading={isLoading || isFetching}
        onDelete={onDelete}
        rowActions={(row) =>
          !row.finished && (
            <Tooltip title={t('bill.end.button')}>
              <IconButton
                size="small"
                onClick={(event) => {
                  event.stopPropagation();
                  setToEnd(row);
                }}
              >
                <EventBusyRounded fontSize="small" />
              </IconButton>
            </Tooltip>
          )
        }
        emptyTitle={t('bill.empty.title', { type: t('bill.types.RECURRING') })}
        emptyText={t('bill.empty.RECURRING')}
        emptyAction={
          <RouterButton
            to="/car/$id/bill/add"
            params={{ id: `${carId}` }}
            search={{ type: BillType.RECURRING }}
            variant="contained"
          >
            {t('bill.add.button')}
          </RouterButton>
        }
      />
      <EndRecurringDialog
        carId={carId}
        bill={toEnd}
        onClose={() => setToEnd(undefined)}
      />
    </Section>
  );
}
