import { PickerValue } from '@mui/x-date-pickers/internals';
import { formatDayjs } from '@/utils/DateUtils';

export default interface DashboardDateRange {
  from?: PickerValue;
  to?: PickerValue;
}

export function convertDateRangeToQuery(range: DashboardDateRange): {
  from?: string;
  to?: string;
} {
  return {
    from: range.from ? formatDayjs(range.from) : undefined,
    to: range.to ? formatDayjs(range.to) : undefined,
  };
}
