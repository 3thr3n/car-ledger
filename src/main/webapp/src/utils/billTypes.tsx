import { BillType, EnergyType } from '@/generated';
import { hasBattery, hasTank } from '@/utils/energy';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import EvStationRounded from '@mui/icons-material/EvStationRounded';
import BuildRounded from '@mui/icons-material/BuildRounded';
import ReceiptLongRounded from '@mui/icons-material/ReceiptLongRounded';
import EventRepeatRounded from '@mui/icons-material/EventRepeatRounded';
import { ReactElement } from 'react';

export type BillPaletteKey =
  'fuel' | 'charging' | 'maintenance' | 'miscellaneous' | 'recurring';

export interface BillTypeMeta {
  type: BillType;
  color: BillPaletteKey;
  /** list of this type below a car */
  listRoute:
    | '/car/$id/bill/fuel'
    | '/car/$id/bill/charging'
    | '/car/$id/bill/maintenance'
    | '/car/$id/bill/miscellaneous'
    | '/car/$id/bill/recurring';
  icon: ReactElement;
  /** whether the CSV import supports this type */
  importable: boolean;
}

export const billTypes: Record<BillType, BillTypeMeta> = {
  [BillType.FUEL]: {
    type: BillType.FUEL,
    color: 'fuel',
    listRoute: '/car/$id/bill/fuel',
    icon: <LocalGasStationRounded fontSize="small" />,
    importable: true,
  },
  [BillType.CHARGING]: {
    type: BillType.CHARGING,
    color: 'charging',
    listRoute: '/car/$id/bill/charging',
    icon: <EvStationRounded fontSize="small" />,
    importable: false,
  },
  [BillType.MAINTENANCE]: {
    type: BillType.MAINTENANCE,
    color: 'maintenance',
    listRoute: '/car/$id/bill/maintenance',
    icon: <BuildRounded fontSize="small" />,
    importable: true,
  },
  [BillType.MISCELLANEOUS]: {
    type: BillType.MISCELLANEOUS,
    color: 'miscellaneous',
    listRoute: '/car/$id/bill/miscellaneous',
    icon: <ReceiptLongRounded fontSize="small" />,
    importable: true,
  },
  [BillType.RECURRING]: {
    type: BillType.RECURRING,
    color: 'recurring',
    listRoute: '/car/$id/bill/recurring',
    icon: <EventRepeatRounded fontSize="small" />,
    importable: false,
  },
};

export const billTypeList = Object.values(billTypes);

/**
 * The entry types of a car: refuels with a tank, charges with a battery, the
 * others always
 */
export function billTypesFor(energyType?: EnergyType): BillTypeMeta[] {
  return billTypeList.filter(
    (meta) =>
      (meta.type !== BillType.FUEL || hasTank(energyType)) &&
      (meta.type !== BillType.CHARGING || hasBattery(energyType)),
  );
}

export function isBillType(value: unknown): value is BillType {
  return (
    typeof value === 'string' &&
    (Object.values(BillType) as string[]).includes(value)
  );
}
