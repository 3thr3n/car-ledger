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
import { Accent, Account } from '@/generated';
import { chooseAccent, useChosenAccent } from '@/hooks/useAccent';
import { ACCENTS } from '@/theme';

/**
 * The accent color of the app, one of the presets. Shown at once and saved
 * with the account, without a connection once it is back.
 */
export default function AppearanceSection({ account }: { account: Account }) {
  const { t } = useTranslation();
  const { mode, systemMode } = useColorScheme();
  const scheme = (mode === 'system' ? systemMode : mode) ?? 'light';

  const chosen = useChosenAccent(account);

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
        value={chosen}
        onChange={(_, value) => chooseAccent(value as Accent)}
        sx={{ gap: 1 }}
      >
        {Object.values(Accent).map((accent) => (
          <FormControlLabel
            key={accent}
            value={accent}
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
