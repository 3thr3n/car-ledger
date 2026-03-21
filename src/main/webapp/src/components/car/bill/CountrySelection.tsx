import { formatVatPercent, supportedVatCountries } from '@/utils/VatCountries';
import { Box, MenuItem, Select, SxProps, Theme } from '@mui/material';
import countries from 'i18n-iso-countries';
import en from 'i18n-iso-countries/langs/en.json';
import ReactCountryFlag from 'react-country-flag';
import { useTranslation } from 'react-i18next';

countries.registerLocale(en);

export default function CountrySelection({
  value,
  onChange,
  sx,
}: {
  value: string;
  onChange: (v: string) => void;
  sx?: SxProps<Theme>;
}) {
  const { i18n } = useTranslation();

  return (
    <Select
      id="CountrySelection"
      fullWidth={true}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      sx={sx}
    >
      {Object.entries(supportedVatCountries).map(([code, rate]) => (
        <MenuItem key={code} value={code}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: '24px 1fr 80px',
              alignItems: 'center',
              width: '100%',
              columnGap: 1,
            }}
          >
            <ReactCountryFlag svg countryCode={code} />
            <span>{countries.getName(code, i18n.language)}</span>
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
