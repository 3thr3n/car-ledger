import { useSyncExternalStore } from 'react';

/** how the app is installed in the browser, each has its own steps */
export type InstallBrowser = 'ios' | 'firefoxAndroid' | 'android' | 'desktop';

/**
 * @param maxTouchPoints iPads report the user agent of a Mac, but have touch
 */
export function detectBrowser(
  userAgent: string,
  maxTouchPoints = 0,
): InstallBrowser {
  if (
    /iPhone|iPad|iPod/.test(userAgent) ||
    (/Macintosh/.test(userAgent) && maxTouchPoints > 1)
  ) {
    return 'ios';
  }
  if (/Android/.test(userAgent)) {
    return /Firefox\//.test(userAgent) ? 'firefoxAndroid' : 'android';
  }
  return 'desktop';
}

export const currentBrowser = () =>
  detectBrowser(navigator.userAgent, navigator.maxTouchPoints);

/** the app runs installed, not in a tab of the browser */
export function isStandalone() {
  return (
    window.matchMedia?.('(display-mode: standalone)').matches ||
    (navigator as Navigator & { standalone?: boolean }).standalone === true
  );
}

/** the event of Chromium browsers to open their install dialog later */
interface BeforeInstallPromptEvent extends Event {
  prompt: () => Promise<void>;
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed' }>;
}

let installPrompt: BeforeInstallPromptEvent | undefined;
const listeners = new Set<() => void>();
const notify = () => listeners.forEach((listener) => listener());

/**
 * Keeps the install dialog of the browser for the button of the app, instead
 * of the bar the browser shows by itself. Called at the start, the event comes
 * early.
 */
export function listenForInstallPrompt() {
  window.addEventListener('beforeinstallprompt', (event) => {
    event.preventDefault();
    installPrompt = event as BeforeInstallPromptEvent;
    notify();
  });
  window.addEventListener('appinstalled', () => {
    installPrompt = undefined;
    notify();
  });
}

const subscribe = (listener: () => void) => {
  listeners.add(listener);
  return () => listeners.delete(listener);
};

/**
 * The browser can install the app with its own dialog (Chromium), else the
 * user follows the steps of the browser
 */
export function useInstallPrompt() {
  const prompt = useSyncExternalStore(subscribe, () => installPrompt);
  return prompt
    ? async () => {
        await prompt.prompt();
        // the event can only be used once
        installPrompt = undefined;
        notify();
      }
    : undefined;
}

const DISMISSED = 'installHintDismissed';

export function isHintDismissed() {
  try {
    return localStorage.getItem(DISMISSED) === 'true';
  } catch {
    return false;
  }
}

export function dismissHint() {
  try {
    localStorage.setItem(DISMISSED, 'true');
  } catch {
    // storage blocked: the hint comes back on the next start
  }
}
