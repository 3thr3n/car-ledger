import { useTranslation } from 'react-i18next';
import React, { useCallback } from 'react';
import {
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  SelectChangeEvent,
} from '@mui/material';

export interface LanguageSwitcherProps {
  drawerMode?: boolean;
}

const languages = [
  { code: 'en', label: 'English' },
  { code: 'de', label: 'Deutsch' },
];

export function LanguageSwitcher({ drawerMode }: LanguageSwitcherProps) {
  const { i18n } = useTranslation();

  const onSelectChange = useCallback((event: SelectChangeEvent) => {
    i18n.changeLanguage(event.target.value as string);
  }, []);

  if (drawerMode) {
    return (
      <div style={{ display: 'flex', gap: 8 }}>
        <button
          onClick={() => i18n.changeLanguage('en')}
          disabled={i18n.language === 'en'}
        >
          EN
        </button>

        <button
          onClick={() => i18n.changeLanguage('de')}
          disabled={i18n.language === 'de'}
        >
          DE
        </button>
      </div>
    );
  }

  return (
    <FormControl size="small" variant="outlined" sx={{ minWidth: 100 }}>
      <InputLabel id="language-select-label">Lang</InputLabel>
      <Select
        labelId="language-select-label"
        value={i18n.language}
        label="Lang"
        onChange={onSelectChange}
      >
        {languages.map((lang) => (
          <MenuItem key={lang.code} value={lang.code}>
            {lang.label}
          </MenuItem>
        ))}
      </Select>
    </FormControl>
  );
}
