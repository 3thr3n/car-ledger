import { Stack } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { toApiDate } from '@/utils/format';

export interface DateRangeFieldsProps {
  from?: string;
  to?: string;
  onChange: (range: { from?: string; to?: string }) => void;
}

/** From and to of a custom period, loaded on demand with the date pickers */
export default function DateRangeFields({
  from,
  to,
  onChange,
}: DateRangeFieldsProps) {
  const { t } = useTranslation();

  return (
    <Stack direction="row" spacing={1.5}>
      <DatePicker
        label={t('dashboard.filter.from')}
        value={from ? dayjs(from) : null}
        maxDate={to ? dayjs(to) : undefined}
        onChange={(value) =>
          onChange({
            from: value?.isValid() ? toApiDate(value) : undefined,
            to,
          })
        }
        slotProps={{ textField: { size: 'small' } }}
      />
      <DatePicker
        label={t('dashboard.filter.to')}
        value={to ? dayjs(to) : null}
        minDate={from ? dayjs(from) : undefined}
        onChange={(value) =>
          onChange({
            from,
            to: value?.isValid() ? toApiDate(value) : undefined,
          })
        }
        slotProps={{ textField: { size: 'small' } }}
      />
    </Stack>
  );
}
