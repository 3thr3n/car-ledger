import { Button, Stack, Typography } from '@mui/material';
import { toast } from 'react-toastify';
import { registerSW } from 'virtual:pwa-register';
import i18n from '@/i18n';

/** a running app looks for a new version this often */
const UPDATE_CHECK = 60 * 60 * 1000;

/**
 * Registers the service worker of the installed app (only in the build). A new
 * version is loaded in the background and waits: the user reloads when it
 * suits, e.g. not in the middle of a form.
 */
export function registerServiceWorker() {
  if (!import.meta.env.PROD || !('serviceWorker' in navigator)) return;

  const update = registerSW({
    onNeedRefresh() {
      toast.info(
        <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
          <Typography variant="body2">{i18n.t('app.update.text')}</Typography>
          <Button size="small" variant="contained" onClick={() => update(true)}>
            {i18n.t('app.update.reload')}
          </Button>
        </Stack>,
        { toastId: 'app-update', autoClose: false, closeOnClick: false },
      );
    },
    onRegisteredSW(_url, registration) {
      if (registration) {
        setInterval(() => registration.update(), UPDATE_CHECK);
      }
    },
  });
}
