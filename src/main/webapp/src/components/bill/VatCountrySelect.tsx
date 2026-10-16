import { ReactNode } from 'react';
import { Box, MenuItem, TextField } from '@mui/material';
import countries from 'i18n-iso-countries';
import { useTranslation } from 'react-i18next';
import { formatVatPercent, supportedVatCountries } from '@/utils/VatCountries';
import { useFormat } from '@/utils/format';
import { flagUrl } from '@/utils/flags';

export default function VatCountrySelect({
  value,
  onChange,
  helperText,
}: {
  helperText?: ReactNode;
  value: string;
  onChange: (code: string) => void;
}) {
  const { t, i18n } = useTranslation();
  const format = useFormat();
  const language = i18n.language.toLowerCase();

  const options = Object.entries(supportedVatCountries)
    .map(([code, rate]) => ({
      code,
      rate,
      name:
        countries.getName(code, language) ??
        countries.getName(code, 'en') ??
        code,
    }))
    .sort((a, b) => a.name.localeCompare(b.name, format.locale));

  return (
    <TextField
      select
      label={t('bill.fields.vatCountry')}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      helperText={helperText ?? t('bill.vatHelp')}
    >
      {options.map(({ code, rate, name }) => (
        <MenuItem key={code} value={code}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: '22px 1fr auto',
              alignItems: 'center',
              columnGap: 1.25,
              width: '100%',
            }}
          >
            <Box
              component="img"
              src={flagUrl(code)}
              alt=""
              loading="lazy"
              sx={{
                width: 22,
                height: 16.5,
                objectFit: 'cover',
                borderRadius: 0.25,
              }}
            />
            <span>{name}</span>
            <Box
              component="span"
              className="tabular"
              sx={{ color: 'text.secondary' }}
            >
              {format.number(
                formatVatPercent(rate),
                Number.isInteger(formatVatPercent(rate)) ? 0 : 1,
              )}{' '}
              %
            </Box>
          </Box>
        </MenuItem>
      ))}
    </TextField>
  );
}
