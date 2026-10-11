import { Button, Stack, Typography } from '@mui/material';
import ErrorOutlineRounded from '@mui/icons-material/ErrorOutlineRounded';
import { useTranslation } from 'react-i18next';
import { RouterButton } from '@/components/ui/links';
import { BackendError } from '@/utils/BackendError';
import { baseUrl } from '@/utils/QueryClient';

export interface ErrorPageProps {
  error?: unknown;
}

export default function ErrorPage({ error }: ErrorPageProps) {
  const { t } = useTranslation();

  const needsLogin = error instanceof BackendError && error.status === 401;
  const message =
    error instanceof BackendError
      ? needsLogin
        ? t('errors.loginRequired')
        : error.message || t('errors.backend')
      : error instanceof Error
        ? error.message
        : t('errors.generic');

  return (
    <Stack
      id="ErrorPage"
      alignItems="center"
      justifyContent="center"
      textAlign="center"
      spacing={2}
      sx={{ flexGrow: 1, py: 10, px: 2 }}
    >
      <ErrorOutlineRounded color="error" sx={{ fontSize: 56 }} />
      <Typography variant="h4">{t('errors.title')}</Typography>
      <Typography color="text.secondary" maxWidth={440}>
        {message}
      </Typography>
      <Stack direction="row" spacing={1}>
        {needsLogin && (
          <Button variant="contained" href={`${baseUrl}/api/auth/login`}>
            {t('button.login')}
          </Button>
        )}
        <RouterButton to="/" variant={needsLogin ? 'outlined' : 'contained'}>
          {t('errors.goHome')}
        </RouterButton>
      </Stack>
    </Stack>
  );
}
