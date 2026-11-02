import { Alert, Box, Button, Link, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import Section from '@/components/ui/Section';
import { isStandalone, useInstallPrompt } from '@/utils/install';
import InstallSteps from '@/components/install/InstallSteps';
import useVersion from '@/hooks/useVersion';
import { reloadToNewest } from '@/utils/serviceWorker';
import { SOURCE_URL } from '@/utils/source';

/** the steps to install the app, also after the hint was dismissed */
export default function InstallSection() {
  const { t } = useTranslation();
  const install = useInstallPrompt();
  const standalone = isStandalone();
  const version = useVersion();

  return (
    <Section
      id="app"
      title={t('install.section.title')}
      subtitle={t(
        standalone ? 'install.section.installed' : 'install.section.text',
      )}
      sx={{ mb: 3 }}
    >
      <Stack spacing={1.5}>
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
        <Typography variant="body2" data-testid="version-details">
          {t('app.versions', {
            app: version.app,
            server: version.server ?? '…',
          })}
        </Typography>
        {/* section 13 of the AGPL: the users can get the source */}
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('app.source')}{' '}
          <Link href={SOURCE_URL} target="_blank" rel="noreferrer">
            {t('app.sourceLink')}
          </Link>
        </Typography>
        {version.outdated ? (
          <Alert
            severity="warning"
            action={
              <Button color="inherit" size="small" onClick={reloadToNewest}>
                {t('app.update.reload')}
              </Button>
            }
          >
            {t('app.update.outdated')}
          </Alert>
        ) : (
          version.server && (
            <Typography variant="body2" sx={{ color: 'success.main' }}>
              {t('app.update.current')}
            </Typography>
          )
        )}
      </Stack>
    </Section>
  );
}
