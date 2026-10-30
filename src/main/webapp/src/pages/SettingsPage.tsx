import {
  Box,
  Button,
  Card,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import { useMutation } from '@tanstack/react-query';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Account, UnitSystem } from '@/generated';
import { deleteMyselfMutation } from '@/generated/@tanstack/react-query.gen';
import useMe from '@/hooks/useMe';
import useSaveSettings from '@/hooks/useSaveSettings';
import { errorMessage } from '@/utils/BackendError';
import queryClient, { baseUrl, localClient } from '@/utils/QueryClient';
import { clearMappings } from '@/utils/csv';
import { markAccountDeleted } from '@/utils/accountDeleted';
import { currencySymbol, DEFAULT_CURRENCY, useFormat } from '@/utils/format';
import Page from '@/components/layout/Page';
import BackupSection, { backupUrl } from '@/components/settings/BackupSection';
import OfflineSection from '@/components/settings/OfflineSection';
import AppearanceSection from '@/components/settings/AppearanceSection';
import NotificationSection from '@/components/settings/NotificationSection';
import InstallSection from '@/components/install/InstallSection';
import PageHeader from '@/components/ui/PageHeader';
import { offlineQueue, offlineStore } from '@/offline';
import { forgetQueries } from '@/offline/persist';
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
      {account && <AppearanceSection account={account} />}
      {account && <NotificationSection />}
      {account && <InstallSection />}
      {account && <BackupSection />}
      {account && <OfflineSection />}
      {account?.name && <DeleteAccount name={account.name} />}
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
  const [unitSystem, setUnitSystem] = useState(
    account.unitSystem ?? UnitSystem.METRIC,
  );

  const settings = useSaveSettings({
    onSuccess: () => {
      // an empty language falls back to the one of the browser on the next start
      if (language !== BROWSER) i18n.changeLanguage(language);
      toast.success(t('settings.saved'));
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
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
      unitSystem,
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
          <Typography
            variant="subtitle1"
            component="h2"
            sx={{
              fontWeight: 650,
              mb: 1.5,
            }}
          >
            {t('settings.entries')}
          </Typography>
          <VatCountrySelect
            value={vatCountry}
            onChange={setVatCountry}
            helperText={t('settings.vatHelp')}
          />
        </Box>

        <Box>
          <Typography
            variant="subtitle1"
            component="h2"
            sx={{
              fontWeight: 650,
              mb: 1.5,
            }}
          >
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
                    <Box
                      component="span"
                      sx={{
                        fontWeight: 600,
                      }}
                    >
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
              label={t('settings.units')}
              value={unitSystem}
              onChange={(e) => setUnitSystem(e.target.value as UnitSystem)}
              helperText={t('settings.unitsHelp')}
            >
              {Object.values(UnitSystem).map((system) => (
                <MenuItem key={system} value={system}>
                  {t(`settings.unitSystems.${system}`)}
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

        <Stack
          direction="row"
          sx={{
            justifyContent: 'flex-end',
          }}
        >
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

function DeleteAccount({ name }: { name: string }) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);
  const [confirmation, setConfirmation] = useState('');

  const mutation = useMutation({
    ...deleteMyselfMutation({ client: localClient }),
    onSuccess: () => {
      // no more requests, the next one would create the account again
      queryClient.cancelQueries();
      queryClient.clear();
      clearMappings();
      markAccountDeleted();
      void forgetQueries(offlineStore);
      void offlineQueue.discardUser(name);
      location.href = `${baseUrl}/api/auth/logout`;
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const close = () => {
    setOpen(false);
    setConfirmation('');
  };

  return (
    <Card sx={{ p: 3, mt: 3, borderColor: 'error.main' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{
          alignItems: { sm: 'center' },
          justifyContent: 'space-between',
        }}
      >
        <Box>
          <Typography
            sx={{
              fontWeight: 600,
            }}
          >
            {t('settings.deleteAccount.title')}
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: 'text.secondary',
            }}
          >
            {t('settings.deleteAccount.text')}
          </Typography>
        </Box>
        <Button
          color="error"
          variant="outlined"
          onClick={() => setOpen(true)}
          sx={{ flexShrink: 0 }}
        >
          {t('settings.deleteAccount.button')}
        </Button>
      </Stack>
      <Dialog open={open} onClose={close} maxWidth="xs" fullWidth>
        <DialogTitle>{t('settings.deleteAccount.confirmTitle')}</DialogTitle>
        <DialogContent>
          <DialogContentText sx={{ mb: 1.5 }}>
            {t('settings.deleteAccount.confirmText')}
          </DialogContentText>
          <DialogContentText sx={{ mb: 1.5 }}>
            {t('settings.deleteAccount.identityProvider')}
          </DialogContentText>
          {/* the last chance to keep the data, e.g. for another instance */}
          <Button
            href={backupUrl}
            size="small"
            startIcon={<FileDownloadRounded />}
            sx={{ mb: 2, ml: -0.5 }}
          >
            {t('settings.deleteAccount.backupFirst')}
          </Button>
          <TextField
            label={t('settings.deleteAccount.typeName', { name })}
            value={confirmation}
            onChange={(e) => setConfirmation(e.target.value)}
            autoComplete="off"
            fullWidth
            autoFocus
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={close} color="inherit">
            {t('button.cancel')}
          </Button>
          <Button
            variant="contained"
            color="error"
            disabled={confirmation.trim() !== name || mutation.isPending}
            onClick={() => mutation.mutate({})}
          >
            {t('settings.deleteAccount.button')}
          </Button>
        </DialogActions>
      </Dialog>
    </Card>
  );
}
