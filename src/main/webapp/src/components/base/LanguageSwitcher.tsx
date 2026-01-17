import { useTranslation } from 'react-i18next';
import React, { useCallback } from 'react';
import {
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  SelectChangeEvent,
} from '@mui/material';
import ReactCountryFlag from 'react-country-flag';

export interface LanguageSwitcherProps {
  drawerMode?: boolean;
}

const languages = [
  { flag: 'US', code: 'EN', label: 'English (US)' },
  { flag: 'DE', code: 'DE', label: 'Deutsch' },
];

export function LanguageSwitcher({ drawerMode }: LanguageSwitcherProps) {
  const { i18n, t } = useTranslation();

  const onSelectChange = useCallback((event: SelectChangeEvent) => {
    i18n.changeLanguage(event.target.value as string);
  }, []);

  if (drawerMode) {
    return (
      <div style={{ display: 'flex', gap: 8 }}>
        {languages.map((lang) => (
          <IconButton
            key={lang.code}
            onClick={() => i18n.changeLanguage(lang.code)}
            disabled={i18n.language === lang.code}
            size="small"
            className={i18n.language === lang.code ? 'secondary-border' : ''}
            sx={{
              border: i18n.language === lang.code ? '2px solid' : 'none',
              borderRadius: 1,
              borderColor: 'primary !important',
            }}
          >
            <ReactCountryFlag svg countryCode={lang.flag} title={lang.label} />
          </IconButton>
        ))}
      </div>
    );
  }

  return (
    <FormControl size="small" variant="outlined" sx={{ minWidth: 80 }}>
      <InputLabel id="language-select-label">{t('app.language')}</InputLabel>
      <Select
        labelId="language-select-label"
        value={i18n.language}
        sx={{
          alignSelf: 'center',
        }}
        onChange={onSelectChange}
      >
        {languages.map((lang) => (
          <MenuItem
            key={lang.code}
            value={lang.code}
            sx={{
              justifyContent: 'center',
            }}
          >
            <ReactCountryFlag svg countryCode={lang.flag} title={lang.label} />
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
