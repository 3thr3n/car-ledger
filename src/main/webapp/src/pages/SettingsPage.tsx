import {
  Box,
  Button,
  Card,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Account } from '@/generated';
import useMe from '@/hooks/useMe';
import useSaveSettings from '@/hooks/useSaveSettings';
import { BackendError } from '@/utils/BackendError';
import { currencySymbol, DEFAULT_CURRENCY, useFormat } from '@/utils/format';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import VatCountrySelect from '@/components/bill/VatCountrySelect';

const currencies = [
  'EUR',
  'USD',
  'GBP',
  'CHF',
  'PLN',
  'CZK',
  'HUF',
  'SEK',
  'NOK',
  'DKK',
  'CAD',
  'AUD',
  'JPY',
];

/** the language of the browser is used without a language in the settings */
const BROWSER = 'browser';
const languages = [BROWSER, 'EN', 'DE'];

export default function SettingsPage() {
  const { t } = useTranslation();
  const { account } = useMe();

  return (
    <Page id="SettingsPage" maxWidth="sm">
      <PageHeader
        title={t('settings.title')}
        subtitle={t('settings.subtitle')}
      />
      {/* keyed, so the form starts with the loaded settings */}
      {account && <SettingsForm key={account.name} account={account} />}
    </Page>
  );
}

function SettingsForm({ account }: { account: Account }) {
  const { t, i18n } = useTranslation();
  const format = useFormat();
  const [vatCountry, setVatCountry] = useState(account.vatCountry ?? 'DE');
  const [currency, setCurrency] = useState(
    account.currency ?? DEFAULT_CURRENCY,
  );
  const [language, setLanguage] = useState(account.language ?? BROWSER);

  const settings = useSaveSettings({
    onSuccess: () => {
      // an empty language falls back to the one of the browser on the next start
      if (language !== BROWSER) i18n.changeLanguage(language);
      toast.success(t('settings.saved'));
    },
    onError: (error) =>
      toast.error(
        error instanceof BackendError && error.message
          ? error.message
          : t('errors.backend'),
      ),
  });

  const currencyName = (code: string) => {
    try {
      return (
        new Intl.DisplayNames([format.locale], { type: 'currency' }).of(code) ??
        code
      );
    } catch {
      return code;
    }
  };

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    settings.save({
      vatCountry,
      currency,
      language: language === BROWSER ? undefined : language,
    });
  };

  const example = new Intl.NumberFormat(format.locale, {
    style: 'currency',
    currency,
  }).format(1234.5);

  return (
    <Card component="form" onSubmit={submit} sx={{ p: 3 }}>
      <Stack spacing={3}>
        <Box>
          <Typography variant="subtitle1" fontWeight={650} sx={{ mb: 1.5 }}>
            {t('settings.entries')}
          </Typography>
          <VatCountrySelect
            value={vatCountry}
            onChange={setVatCountry}
            helperText={t('settings.vatHelp')}
          />
        </Box>

        <Box>
          <Typography variant="subtitle1" fontWeight={650} sx={{ mb: 1.5 }}>
            {t('settings.display')}
          </Typography>
          <Stack spacing={2.5}>
            <TextField
              select
              label={t('settings.currency')}
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              helperText={t('settings.currencyHelp', { example })}
            >
              {currencies.map((code) => (
                <MenuItem key={code} value={code}>
                  <Box
                    sx={{
                      display: 'grid',
                      gridTemplateColumns: '48px 1fr auto',
                      width: '100%',
                      columnGap: 1,
                    }}
                  >
                    <Box component="span" fontWeight={600}>
                      {code}
                    </Box>
                    <span>{currencyName(code)}</span>
                    <Box component="span" sx={{ color: 'text.secondary' }}>
                      {currencySymbol(format.locale, code)}
                    </Box>
                  </Box>
                </MenuItem>
              ))}
            </TextField>
            <TextField
              select
              label={t('settings.language')}
              value={language}
              onChange={(e) => setLanguage(e.target.value)}
            >
              {languages.map((code) => (
                <MenuItem key={code} value={code}>
                  {t(`settings.languages.${code}`)}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </Box>

        <Stack direction="row" justifyContent="flex-end">
          <Button
            type="submit"
            variant="contained"
            disabled={settings.isPending}
          >
            {t('button.save')}
          </Button>
        </Stack>
      </Stack>
    </Card>
  );
}
