import { useEffect, useState, useSyncExternalStore } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Accent, Account } from '@/generated';
import { ACCENTS } from '@/theme';
import useMe from '@/hooks/useMe';
import useOnline from '@/hooks/useOnline';
import useSaveSettings from '@/hooks/useSaveSettings';
import { BackendError, errorMessage } from '@/utils/BackendError';

/** the accent of the last account on this device, until the account is loaded */
const KEY = 'accent';
/** chosen but not saved yet, e.g. without a connection */
const PENDING = 'accentPending';
/** the color of the bar, set by public/theme-color.js before the app starts */
const THEME_COLOR_KEY = 'themeColor';

function stored(key: string): Accent | undefined {
  try {
    const accent = localStorage.getItem(key);
    if (accent && accent in ACCENTS) return accent as Accent;
  } catch {
    // no storage, e.g. blocked
  }
  return undefined;
}

function store(key: string, value?: string) {
  try {
    if (value) {
      localStorage.setItem(key, value);
    } else {
      localStorage.removeItem(key);
    }
  } catch {
    // only a convenience
  }
}

const listeners = new Set<() => void>();
let pending = stored(PENDING);

function setPending(accent?: Accent) {
  pending = accent;
  store(PENDING, accent);
  listeners.forEach((listener) => listener());
}

function usePending() {
  return useSyncExternalStore(
    (listener) => {
      listeners.add(listener);
      return () => listeners.delete(listener);
    },
    () => pending,
  );
}

/**
 * Chooses the accent of the account: shown at once, saved when the app is
 * online, also after a restart. The last choice wins.
 */
export function chooseAccent(accent: Accent) {
  setPending(accent);
}

/** the accent shown as chosen, the one not saved yet first */
export function useChosenAccent(account: Account): Accent {
  return usePending() ?? account.accent;
}

/**
 * The accent color of the account. The one of the last account on this device
 * is used while loading, so the app doesn't start in the default color. A
 * chosen one is saved here, once the account is loaded and the app online.
 * Used once, by the theme of the app.
 */
export default function useAccent(): Accent {
  const { t } = useTranslation();
  const { account } = useMe();
  const online = useOnline();
  const chosen = usePending();
  const [cached] = useState(() => stored(KEY) ?? Accent.TEAL);
  const accent = chosen ?? account?.accent ?? cached;

  // the saved choice is dropped below, once the account has it: the cached
  // account is updated a tick later, without it the old accent would flash
  const settings = useSaveSettings({
    onError: (error) => {
      // without a connection it is sent later
      if (error instanceof BackendError && error.status === 0) return;
      setPending(undefined);
      toast.error(errorMessage(error, t('errors.backend')));
    },
  });

  useEffect(() => {
    if (!chosen || !account) return;
    if (chosen === account.accent) {
      setPending(undefined);
      return;
    }
    // a newer choice is sent with the next run
    if (!online || settings.isPending) return;
    settings.save({
      vatCountry: account.vatCountry,
      currency: account.currency,
      language: account.language,
      unitSystem: account.unitSystem,
      accent: chosen,
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [chosen, account, online, settings.isPending]);

  useEffect(() => store(KEY, accent), [accent]);

  // the bar of the browser on phones, and of the installed app: the service
  // worker puts it into the manifest, public/theme-color.js into the page on
  // the next start
  useEffect(() => {
    const color = ACCENTS[accent].light.main;
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', color);
    store(THEME_COLOR_KEY, color);
    keepThemeColor(color);
  }, [accent]);

  return accent;
}

/** same as THEME_COLOR of src/sw/sw.ts, the worker has its own build */
const THEME_COLOR = '/theme-color';
const SETTINGS_CACHE = 'car-ledger-settings';

function keepThemeColor(color: string) {
  if (typeof caches === 'undefined') return;
  caches
    .open(SETTINGS_CACHE)
    .then((cache) => cache.put(THEME_COLOR, new Response(color)))
    .catch(() => undefined);
}
