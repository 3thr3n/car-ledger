import { Button, Stack, Typography } from '@mui/material';
import ErrorOutlineRounded from '@mui/icons-material/ErrorOutlineRounded';
import { useTranslation } from 'react-i18next';
import { RouterButton } from '@/components/ui/links';
import { BackendError } from '@/utils/BackendError';
import { baseUrl } from '@/utils/QueryClient';

export interface ErrorPageProps {
  error?: unknown;
  /** error of the OIDC provider during the login, offers to log in again */
  loginError?: string;
}

export default function ErrorPage({ error, loginError }: ErrorPageProps) {
  const { t } = useTranslation();

  const needsLogin =
    loginError !== undefined ||
    (error instanceof BackendError && error.status === 401);
  const message =
    loginError !== undefined
      ? t('errors.loginFailed', { reason: loginError })
      : error instanceof BackendError
        ? needsLogin
          ? t('errors.loginRequired')
          : error.message || t('errors.backend')
        : error instanceof Error
          ? error.message
          : t('errors.generic');

  return (
    <Stack
      id="ErrorPage"
      spacing={2}
      sx={{
        alignItems: 'center',
        justifyContent: 'center',
        textAlign: 'center',
        flexGrow: 1,
        py: 10,
        px: 2,
      }}
    >
      <ErrorOutlineRounded color="error" sx={{ fontSize: 56 }} />
      <Typography variant="h4">{t('errors.title')}</Typography>
      <Typography
        sx={{
          color: 'text.secondary',
          maxWidth: 440,
        }}
      >
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
