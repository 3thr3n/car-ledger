const KEY = 'accountDeleted';

/** remembers the deletion over the logout, which leaves the app */
export function markAccountDeleted() {
  try {
    sessionStorage.setItem(KEY, 'true');
  } catch {
    // the confirmation on the landing page is only a nicety
  }
}

/** @return whether the account was just deleted */
export function wasAccountDeleted(): boolean {
  try {
    return sessionStorage.getItem(KEY) !== null;
  } catch {
    return false;
  }
}

/** the notice is shown only once */
export function forgetAccountDeleted() {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // nothing stored without storage
  }
}
