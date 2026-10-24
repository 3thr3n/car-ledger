import { useEffect, useState } from 'react';
import { Accent } from '@/generated';
import { ACCENTS } from '@/theme';
import useMe from '@/hooks/useMe';

/** the accent of the last account on this device, until the account is loaded */
const KEY = 'accent';

function cachedAccent(): Accent {
  try {
    const accent = localStorage.getItem(KEY);
    if (accent && accent in ACCENTS) return accent as Accent;
  } catch {
    // no storage, e.g. blocked: the default until the account is loaded
  }
  return Accent.TEAL;
}

/**
 * The accent color of the account. The one of the last account on this device
 * is used while loading, so the app doesn't start in the default color.
 */
export default function useAccent(): Accent {
  const { account } = useMe();
  const [cached] = useState(cachedAccent);
  const accent = account?.accent ?? cached;

  useEffect(() => {
    if (!account?.accent) return;
    try {
      localStorage.setItem(KEY, account.accent);
    } catch {
      // only a convenience
    }
  }, [account?.accent]);

  // the bar of the browser on phones
  useEffect(() => {
    document
      .querySelector('meta[name="theme-color"]')
      ?.setAttribute('content', ACCENTS[accent].light.main);
  }, [accent]);

  return accent;
}
