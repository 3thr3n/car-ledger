import { Box, Button, Card, Stack, Typography } from '@mui/material';
import InstallMobileRounded from '@mui/icons-material/InstallMobileRounded';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  currentBrowser,
  dismissHint,
  isHintDismissed,
  isStandalone,
  useInstallPrompt,
} from '@/utils/install';
import InstallSteps from '@/components/install/InstallSteps';

/**
 * Tells the users of phones that CarLedger can be installed. Not in the
 * installed app, and gone for good on this device after "Not now".
 */
export default function InstallHint() {
  const { t } = useTranslation();
  const install = useInstallPrompt();
  const [hidden, setHidden] = useState(
    () => isHintDismissed() || isStandalone() || currentBrowser() === 'desktop',
  );
  const [showSteps, setShowSteps] = useState(false);

  if (hidden) return null;

  const close = () => {
    dismissHint();
    setHidden(true);
  };

  return (
    <Card sx={{ p: 2, mb: 2 }} data-testid="install-hint">
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
        <InstallMobileRounded color="primary" sx={{ mt: 0.25 }} />
        <Box sx={{ flexGrow: 1 }}>
          <Typography sx={{ fontWeight: 600 }}>
            {t('install.hint.title')}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('install.hint.text')}
          </Typography>
          {showSteps && (
            <Box sx={{ mt: 1 }}>
              <InstallSteps />
            </Box>
          )}
          <Stack direction="row" spacing={1} sx={{ mt: 1.5 }}>
            {install ? (
              <Button
                size="small"
                variant="contained"
                onClick={() => install().then(close)}
              >
                {t('install.install')}
              </Button>
            ) : (
              !showSteps && (
                <Button
                  size="small"
                  variant="contained"
                  onClick={() => setShowSteps(true)}
                >
                  {t('install.hint.showMe')}
                </Button>
              )
            )}
            <Button size="small" color="inherit" onClick={close}>
              {t('install.hint.later')}
            </Button>
          </Stack>
        </Box>
      </Stack>
    </Card>
  );
}
