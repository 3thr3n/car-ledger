import { formatVatPercent, supportedVatCountries } from '@/utils/VatCountries';
import { Box, MenuItem, Select } from '@mui/material';
import countries from 'i18n-iso-countries';
import en from 'i18n-iso-countries/langs/en.json';
import ReactCountryFlag from 'react-country-flag';

countries.registerLocale(en);

export default function CountrySelection({
  value,
  onChange,
}: {
  value: string;
  onChange: (v: string) => void;
}) {
  return (
    <Select value={value} onChange={(e) => onChange(e.target.value)}>
      {Object.entries(supportedVatCountries).map(([code, rate]) => (
        <MenuItem key={code} value={code}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: '24px 1fr 80px',
              alignItems: 'center',
              columnGap: 1,
            }}
          >
            <ReactCountryFlag svg countryCode={code} />
            <span>{countries.getName(code, 'en')}</span>
            <span
              style={{ textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}
            >
              {formatVatPercent(rate).toFixed(2)} %
            </span>
          </Box>
        </MenuItem>
      ))}
    </Select>
  );
}
