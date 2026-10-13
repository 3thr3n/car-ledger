import { Grid } from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import { Dayjs } from 'dayjs';
import { useTranslation } from 'react-i18next';
import useVatCountry from '@/hooks/useVatCountry';
import VatCountrySelect from './VatCountrySelect';

/**
 * First row of the bill forms: the date of the receipt and the country of its
 * VAT, as two items of the surrounding grid
 *
 * @param error shown below the date
 */
export default function DateVatFields({
  date,
  onDateChange,
  vat,
  error,
}: {
  date: Dayjs | null;
  onDateChange: (date: Dayjs | null) => void;
  vat: ReturnType<typeof useVatCountry>;
  error?: string;
}) {
  const { t } = useTranslation();

  return (
    <>
      <Grid size={{ xs: 12, sm: 6 }}>
        <DatePicker
          label={t('bill.fields.date')}
          value={date}
          onChange={onDateChange}
          disableFuture
          slotProps={{
            textField: {
              required: true,
              error: !!error,
              helperText: error,
            },
          }}
        />
      </Grid>
      <Grid size={{ xs: 12, sm: 6 }}>
        <VatCountrySelect value={vat.country} onChange={vat.setCountry} />
      </Grid>
    </>
  );
}
