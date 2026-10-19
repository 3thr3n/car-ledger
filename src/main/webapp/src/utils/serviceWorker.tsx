import { Button, Stack, Typography } from '@mui/material';
import { toast } from 'react-toastify';
import { registerSW } from 'virtual:pwa-register';
import i18n from '@/i18n';

/** a running app looks for a new version this often */
const UPDATE_CHECK = 60 * 60 * 1000;

/** activates the waiting version and reloads, set once registered */
let update: ((reload?: boolean) => Promise<void>) | undefined;
/** a new version is loaded and waits */
let waiting = false;
/** the user asked for the new version before it was loaded */
let applyWhenReady = false;
/** a new version found this soon after the start replaces the page at once */
const AT_START = 30 * 1000;
/** something was typed: a reload would lose it, the user decides */
let typed = false;

/**
 * Without anything typed, a new version needs no question: right after the
 * start (a reload or a new start of the app shows the newest version, not the
 * one of the last visit) or while the app is in the background.
 */
const startsUnasked = () =>
  !typed &&
  (performance.now() < AT_START || document.visibilityState === 'hidden');

/**
 * Registers the service worker of the installed app (only in the build). A new
 * version is loaded in the background. Right after the start or in the
 * background it replaces the page; later it waits and the user reloads when it
 * suits, e.g. not in the middle of a form. A reload alone keeps the version: the
 * old service worker serves the page until the new one takes over.
 */
export function registerServiceWorker() {
  if (!import.meta.env.PROD || !('serviceWorker' in navigator)) return;

  document.addEventListener(
    'input',
    () => {
      typed = true;
    },
    { capture: true, once: true },
  );
  // the app goes to the background: a waiting version starts there, the user
  // sees it when coming back
  document.addEventListener('visibilitychange', () => {
    if (waiting && startsUnasked()) void update?.(true);
  });

  update = registerSW({
    onNeedRefresh() {
      waiting = true;
      if (applyWhenReady || startsUnasked()) {
        update?.(true);
        return;
      }
      toast.info(
        <Stack spacing={1} sx={{ alignItems: 'flex-start' }}>
          <Typography variant="body2">{i18n.t('app.update.text')}</Typography>
          <Button size="small" variant="contained" onClick={reloadToNewest}>
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

/**
 * Starts the newest version: the waiting one, else the service worker looks
 * for it now and it starts once loaded. Without a service worker (dev server)
 * a reload.
 */
export async function reloadToNewest() {
  if (update && waiting) {
    await update(true);
    return;
  }
  const registration = await navigator.serviceWorker?.getRegistration();
  if (!update || !registration) {
    location.reload();
    return;
  }
  applyWhenReady = true;
  await registration.update();
  // nothing new to load: the cached app is the newest of the server already
  if (!registration.installing && !registration.waiting) {
    location.reload();
  }
}
