import { Chip } from '@mui/material';
import { GridColDef, GridValidRowModel } from '@mui/x-data-grid';
import { useTranslation } from 'react-i18next';
import {
  Bill,
  FuelBill,
  MaintenanceBill,
  MiscellaneousBill,
  RecurringBill,
} from '@/generated';
import { hasValue, useFormat } from '@/utils/format';
import TypeChip from '@/components/ui/TypeChip';
import { MobileRow } from './BillTable';

interface Columns<R extends GridValidRowModel> {
  columns: GridColDef<R>[];
  mobileRow: (row: R) => MobileRow;
}

const money = { align: 'right', headerAlign: 'right', minWidth: 110 } as const;

export function useLedgerColumns(): Columns<Bill> {
  const { t } = useTranslation();
  const f = useFormat();

  return {
    columns: [
      {
        field: 'date',
        headerName: t('bill.fields.date'),
        minWidth: 130,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'type',
        headerName: t('bill.fields.type'),
        minWidth: 160,
        flex: 1,
        renderCell: ({ row }) => <TypeChip type={row.type} />,
      },
      {
        field: 'netAmount',
        headerName: t('bill.fields.netAmount'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
      },
      {
        field: 'ustAmount',
        headerName: t('bill.fields.ustAmount'),
        ...money,
        valueGetter: (value: number, row) => ({
          amount: value,
          rate: row.vatRate,
        }),
        valueFormatter: (value: { amount?: number; rate?: number }) =>
          value.rate
            ? `${f.money(value.amount)} (${value.rate} %)`
            : f.money(value.amount),
        minWidth: 150,
      },
      {
        field: 'total',
        headerName: t('bill.fields.total'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
        cellClassName: 'strong',
      },
    ],
    mobileRow: (row) => ({
      primary: f.date(row.date),
      badge: <TypeChip type={row.type} />,
      secondary: `${t('bill.fields.netAmount')} ${f.money(row.netAmount)}`,
      amount: f.money(row.total),
    }),
  };
}

export function useFuelColumns(): Columns<FuelBill> {
  const { t } = useTranslation();
  const f = useFormat();

  return {
    columns: [
      {
        field: 'date',
        headerName: t('bill.fields.date'),
        minWidth: 130,
        flex: 1,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'unit',
        headerName: t('bill.fields.unit'),
        ...money,
        valueFormatter: (value: number) => f.unit(value, 'l'),
      },
      {
        field: 'pricePerUnit',
        headerName: t('bill.fields.pricePerUnit'),
        ...money,
        minWidth: 120,
        valueFormatter: (value: number) => f.unit(value, 'ct/l', 1),
      },
      {
        field: 'distance',
        headerName: t('bill.fields.distance'),
        ...money,
        valueFormatter: (value: number) =>
          hasValue(value) ? f.unit(value, 'km', 1) : '—',
      },
      {
        field: 'avgConsumption',
        headerName: t('bill.fields.avgConsumption'),
        ...money,
        minWidth: 140,
        valueFormatter: (value: number) =>
          hasValue(value) ? f.unit(value, 'l/100km') : '—',
      },
      {
        field: 'estimateConsumption',
        headerName: t('bill.fields.estimateShort'),
        ...money,
        minWidth: 140,
        valueFormatter: (value: number) =>
          hasValue(value) ? f.unit(value, 'l/100km', 1) : '—',
      },
      {
        field: 'total',
        headerName: t('bill.fields.total'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
        cellClassName: 'strong',
      },
    ],
    mobileRow: (row) => ({
      primary: f.date(row.date),
      secondary: [
        f.unit(row.unit, 'l'),
        f.unit(row.pricePerUnit, 'ct/l', 1),
        hasValue(row.avgConsumption)
          ? f.unit(row.avgConsumption, 'l/100km')
          : undefined,
      ]
        .filter(Boolean)
        .join(' · '),
      amount: f.money(row.total),
    }),
  };
}

export function useMaintenanceColumns(): Columns<MaintenanceBill> {
  const { t } = useTranslation();
  const f = useFormat();

  return {
    columns: [
      {
        field: 'date',
        headerName: t('bill.fields.date'),
        minWidth: 130,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'workshop',
        headerName: t('bill.fields.workshop'),
        minWidth: 140,
        flex: 1,
      },
      {
        field: 'description',
        headerName: t('bill.fields.description'),
        minWidth: 180,
        flex: 2,
      },
      {
        field: 'odometer',
        headerName: t('bill.fields.odometer'),
        ...money,
        valueFormatter: (value: number) =>
          hasValue(value) ? f.unit(value, 'km', 0) : '—',
      },
      {
        field: 'laborCost',
        headerName: t('bill.fields.laborCost'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
      },
      {
        field: 'partsCost',
        headerName: t('bill.fields.partsCost'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
      },
      {
        field: 'total',
        headerName: t('bill.fields.total'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
        cellClassName: 'strong',
      },
    ],
    mobileRow: (row) => ({
      primary: row.description || row.workshop || f.date(row.date),
      secondary: [f.date(row.date), row.workshop].filter(Boolean).join(' · '),
      amount: f.money(row.total),
    }),
  };
}

export function useMiscellaneousColumns(): Columns<MiscellaneousBill> {
  const { t } = useTranslation();
  const f = useFormat();

  return {
    columns: [
      {
        field: 'date',
        headerName: t('bill.fields.date'),
        minWidth: 130,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'description',
        headerName: t('bill.fields.description'),
        minWidth: 200,
        flex: 1,
      },
      {
        field: 'total',
        headerName: t('bill.fields.total'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
        cellClassName: 'strong',
      },
    ],
    mobileRow: (row) => ({
      primary: row.description || t('bill.types.MISCELLANEOUS'),
      secondary: f.date(row.date),
      amount: f.money(row.total),
    }),
  };
}

function StatusChip({ finished }: { finished?: boolean }) {
  const { t } = useTranslation();
  return (
    <Chip
      size="small"
      variant="outlined"
      color={finished ? 'default' : 'success'}
      label={finished ? t('bill.status.finished') : t('bill.status.running')}
    />
  );
}

export function useRecurringColumns(): Columns<RecurringBill> {
  const { t } = useTranslation();
  const f = useFormat();

  return {
    columns: [
      {
        field: 'name',
        headerName: t('bill.fields.name'),
        minWidth: 160,
        flex: 1,
      },
      {
        field: 'category',
        headerName: t('bill.fields.category'),
        minWidth: 120,
        valueFormatter: (value: string) =>
          value ? t(`bill.categories.${value}`) : '—',
      },
      {
        field: 'finished',
        headerName: t('bill.fields.status'),
        minWidth: 120,
        renderCell: ({ row }) => <StatusChip finished={row.finished} />,
      },
      {
        field: 'startDate',
        headerName: t('bill.fields.startDate'),
        minWidth: 120,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'nextDueDate',
        headerName: t('bill.fields.nextDueDate'),
        minWidth: 120,
        valueGetter: (value: string, row) => (row.finished ? undefined : value),
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'endDate',
        headerName: t('bill.fields.endDate'),
        minWidth: 120,
        valueFormatter: (value: string) => f.date(value),
      },
      {
        field: 'amount',
        headerName: t('bill.fields.amount'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
      },
      {
        field: 'total',
        headerName: t('bill.fields.paidTotal'),
        ...money,
        valueFormatter: (value: number) => f.money(value),
        cellClassName: 'strong',
      },
    ],
    mobileRow: (row) => ({
      primary: row.name,
      badge: <StatusChip finished={row.finished} />,
      secondary: row.finished
        ? `${t('bill.fields.amount')} ${f.money(row.amount)}`
        : `${t('bill.fields.nextDueDate')} ${f.date(row.nextDueDate)} · ${f.money(row.amount)}`,
      amount: f.money(row.total),
    }),
  };
}
