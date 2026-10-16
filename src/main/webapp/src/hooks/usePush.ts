import { useMutation, useQuery } from '@tanstack/react-query';
import { useCallback, useEffect, useState } from 'react';
import {
  getMyPushOptions,
  getMyPushQueryKey,
  subscribeMyPushMutation,
  unsubscribeMyPushMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { currentBrowser, isStandalone } from '@/utils/install';

/**
 * - unavailable: the server sends no push messages
 * - unsupported: the browser cannot receive them (or the dev server, which has
 *   no service worker)
 * - install: an iPhone or iPad receives them only in the installed app
 * - denied: the user blocked the notifications in the browser
 */
export type PushState =
  | 'loading'
  | 'unavailable'
  | 'unsupported'
  | 'install'
  | 'denied'
  | 'off'
  | 'on';

const supported = () =>
  'serviceWorker' in navigator &&
  'PushManager' in window &&
  'Notification' in window;

/** the key of the server as the browser wants it */
function keyBytes(base64url: string) {
  const base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
  const binary = atob(
    base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '='),
  );
  return Uint8Array.from(binary, (char) => char.charCodeAt(0));
}

function sameKey(subscription: PushSubscription, key: Uint8Array) {
  const current = subscription.options.applicationServerKey;
  if (!current) return false;
  const bytes = new Uint8Array(current);
  return (
    bytes.length === key.length && bytes.every((byte, i) => byte === key[i])
  );
}

async function registration() {
  if (!supported()) return undefined;
  return navigator.serviceWorker.getRegistration();
}

const refresh = () =>
  queryClient.invalidateQueries({
    queryKey: getMyPushQueryKey({ client: localClient }),
  });

/**
 * Push messages on this device: the subscription of the browser, kept in sync
 * with the server
 */
export default function usePush() {
  const { data: settings } = useQuery(
    getMyPushOptions({ client: localClient }),
  );
  const [subscription, setSubscription] = useState<
    PushSubscription | null | undefined
  >(undefined);
  const [hasWorker, setHasWorker] = useState<boolean>();
  const [permission, setPermission] = useState(
    'Notification' in window ? Notification.permission : 'denied',
  );

  const save = useMutation(subscribeMyPushMutation({ client: localClient }));
  const remove = useMutation(
    unsubscribeMyPushMutation({ client: localClient }),
  );

  const send = useCallback(
    async (current: PushSubscription) => {
      const json = current.toJSON();
      await save.mutateAsync({
        body: {
          endpoint: json.endpoint,
          p256dh: json.keys?.p256dh,
          auth: json.keys?.auth,
        },
      });
      await refresh();
    },
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [],
  );

  // the subscription of this browser; told to the server again, e.g. after a
  // login with another account on this device
  useEffect(() => {
    if (!settings?.publicKey) return;
    let active = true;
    (async () => {
      const worker = await registration();
      if (!active) return;
      setHasWorker(!!worker);
      const current = (await worker?.pushManager.getSubscription()) ?? null;
      if (!active) return;
      if (current && sameKey(current, keyBytes(settings.publicKey))) {
        setSubscription(current);
        send(current).catch(() => undefined);
      } else {
        // a subscription for an older key of the server receives nothing
        await current?.unsubscribe();
        setSubscription(null);
      }
    })();
    return () => {
      active = false;
    };
  }, [settings?.publicKey, send]);

  const subscribe = async () => {
    if (!settings?.publicKey) return;
    const granted = await Notification.requestPermission();
    setPermission(granted);
    if (granted !== 'granted') return;
    const worker = await navigator.serviceWorker.ready;
    const created = await worker.pushManager.subscribe({
      userVisibleOnly: true,
      applicationServerKey: keyBytes(settings.publicKey),
    });
    await send(created);
    setSubscription(created);
  };

  const unsubscribe = async () => {
    if (!subscription) return;
    await remove.mutateAsync({ body: { endpoint: subscription.endpoint } });
    await subscription.unsubscribe();
    setSubscription(null);
    await refresh();
  };

  let state: PushState;
  if (!settings) state = 'loading';
  else if (!settings.available) state = 'unavailable';
  else if (!supported() || hasWorker === false)
    state =
      currentBrowser() === 'ios' && !isStandalone() ? 'install' : 'unsupported';
  else if (hasWorker === undefined || subscription === undefined)
    state = 'loading';
  else if (subscription) state = 'on';
  else if (permission === 'denied') state = 'denied';
  else state = 'off';

  return {
    state,
    /** browsers of the account, also on other devices */
    browsers: settings?.browsers ?? 0,
    subscribe,
    unsubscribe,
  };
}
