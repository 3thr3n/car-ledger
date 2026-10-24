import {
  Box,
  Card,
  FormControlLabel,
  Radio,
  RadioGroup,
  Typography,
  useColorScheme,
} from '@mui/material';
import CheckRounded from '@mui/icons-material/CheckRounded';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Accent, Account } from '@/generated';
import { getMyselfQueryKey } from '@/generated/@tanstack/react-query.gen';
import useSaveSettings from '@/hooks/useSaveSettings';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { ACCENTS } from '@/theme';

/**
 * The accent color of the app, one of the presets. Saved with the account at
 * once, like the theme in the sidebar.
 */
export default function AppearanceSection({ account }: { account: Account }) {
  const { t } = useTranslation();
  const { mode, systemMode } = useColorScheme();
  const scheme = (mode === 'system' ? systemMode : mode) ?? 'light';

  const key = getMyselfQueryKey({ client: localClient });
  const settings = useSaveSettings({
    onError: (error) => {
      // back to the saved one
      void queryClient.invalidateQueries({ queryKey: key });
      toast.error(errorMessage(error, t('errors.backend')));
    },
  });

  const choose = (accent: Accent) => {
    // shown at once, not after the answer
    queryClient.setQueryData(key, { ...account, accent });
    settings.save({
      vatCountry: account.vatCountry,
      currency: account.currency,
      language: account.language,
      unitSystem: account.unitSystem,
      accent,
    });
  };

  const swatch = (accent: Accent, checked: boolean) => {
    const { main, contrastText } = ACCENTS[accent][scheme];
    return (
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: '50%',
          bgcolor: main,
          color: contrastText,
          display: 'grid',
          placeItems: 'center',
          // a ring around the chosen one, in the color of the text
          boxShadow: checked
            ? (theme) =>
                `0 0 0 2px ${theme.vars.palette.background.paper}, 0 0 0 4px ${theme.vars.palette.text.primary}`
            : 'none',
        }}
      >
        {checked && <CheckRounded fontSize="small" />}
      </Box>
    );
  };

  return (
    <Card sx={{ p: 3, mt: 3 }} data-testid="appearance">
      <Typography sx={{ fontWeight: 600 }} id="accent-label">
        {t('settings.appearance.title')}
      </Typography>
      <Typography variant="body2" sx={{ color: 'text.secondary', mb: 2 }}>
        {t('settings.appearance.text')}
      </Typography>
      <RadioGroup
        row
        aria-labelledby="accent-label"
        value={account.accent}
        onChange={(_, value) => choose(value as Accent)}
        sx={{ gap: 1 }}
      >
        {Object.values(Accent).map((accent) => (
          <FormControlLabel
            key={accent}
            value={accent}
            disabled={settings.isPending}
            label={t(`settings.appearance.accents.${accent}`)}
            control={
              <Radio
                icon={swatch(accent, false)}
                checkedIcon={swatch(accent, true)}
              />
            }
            sx={{ mr: 1.5 }}
          />
        ))}
      </RadioGroup>
    </Card>
  );
}
