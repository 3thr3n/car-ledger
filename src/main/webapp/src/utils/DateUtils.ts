import dayjs, { Dayjs } from 'dayjs';
import { PickerValue } from '@mui/x-date-pickers/internals';

export function formatDayjs(date: Dayjs | PickerValue): string {
  return dayjs(date).format('YYYY-MM-DD');
}
