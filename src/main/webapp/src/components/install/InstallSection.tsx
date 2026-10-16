import { Box, Button, Card, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { isStandalone, useInstallPrompt } from '@/utils/install';
import InstallSteps from '@/components/install/InstallSteps';

/** the steps to install the app, also after the hint was dismissed */
export default function InstallSection() {
  const { t } = useTranslation();
  const install = useInstallPrompt();
  const standalone = isStandalone();

  return (
    <Card sx={{ p: 3, mt: 3 }}>
      <Stack spacing={1.5}>
        <Box>
          <Typography sx={{ fontWeight: 600 }}>
            {t('install.section.title')}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t(
              standalone ? 'install.section.installed' : 'install.section.text',
            )}
          </Typography>
        </Box>
        {!standalone &&
          (install ? (
            <Box>
              <Button variant="outlined" onClick={() => install()}>
                {t('install.install')}
              </Button>
            </Box>
          ) : (
            <InstallSteps />
          ))}
      </Stack>
    </Card>
  );
}
