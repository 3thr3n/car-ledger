import { Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { currentBrowser } from '@/utils/install';

/** how to install the app in the browser at hand */
export default function InstallSteps() {
  const { t } = useTranslation();
  return (
    <Typography variant="body2" data-testid="install-steps">
      {t(`install.steps.${currentBrowser()}`)}
    </Typography>
  );
}
