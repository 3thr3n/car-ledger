/// <reference lib="webworker" />
import { clientsClaim } from 'workbox-core';
import {
  cleanupOutdatedCaches,
  createHandlerBoundToURL,
  precacheAndRoute,
  PrecacheEntry,
} from 'workbox-precaching';
import { NavigationRoute, registerRoute } from 'workbox-routing';

declare const self: ServiceWorkerGlobalScope & {
  __WB_MANIFEST: (PrecacheEntry | string)[];
};

/**
 * Service worker of the installed app: the files of the build (the list is
 * injected by vite-plugin-pwa) come from the cache, so the app starts without
 * waiting for the network. The data of the API isn't cached here, only the
 * documents of the modifications the app keeps (see offline/documents.ts) are
 * served from their cache without a connection.
 */
precacheAndRoute(self.__WB_MANIFEST);
cleanupOutdatedCaches();

// the pages of the app are the cached index.html, not the paths of the server:
// the login and logout (/api/auth/...), the API and the health checks (/q/...)
registerRoute(
  new NavigationRoute(createHandlerBoundToURL('index.html'), {
    denylist: [/^\/api\//, /^\/q\//],
  }),
);

// same as DOCUMENTS_CACHE of offline/documents.ts, the worker has its own build
const DOCUMENTS_CACHE = 'modification-documents';

// a kept document opens from the network first, without a connection from the
// cache, e.g. in an underground car park
registerRoute(
  ({ url }) => /^\/api\/attachment\/\d+\/\d+\/content$/.test(url.pathname),
  async ({ request }) => {
    try {
      return await fetch(request);
    } catch (error) {
      const kept = await caches.match(request.url, {
        cacheName: DOCUMENTS_CACHE,
      });
      if (kept) return kept;
      throw error;
    }
  },
);

// a new version waits until the user reloads (see registerServiceWorker)
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') self.skipWaiting();
});
clientsClaim();

/** a notification of the server (see Notifications.java) */
interface PushPayload {
  title?: string;
  body?: string;
  /** page of the app it opens */
  url?: string;
  /** a newer one with the same tag replaces the older */
  tag?: string;
}

self.addEventListener('push', (event) => {
  let payload: PushPayload;
  try {
    payload = event.data?.json() ?? {};
  } catch {
    payload = { body: event.data?.text() };
  }
  event.waitUntil(
    self.registration.showNotification(payload.title ?? 'CarLedger', {
      body: payload.body,
      tag: payload.tag,
      icon: '/icons/icon-192.png',
      data: { url: payload.url ?? '/dashboard' },
    }),
  );
});

// a click focuses the app and opens the page of the notification
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  const url = new URL(
    event.notification.data?.url ?? '/dashboard',
    self.location.origin,
  ).href;
  event.waitUntil(
    (async () => {
      const windows = await self.clients.matchAll({
        type: 'window',
        includeUncontrolled: true,
      });
      const open = windows.find(
        (client) => new URL(client.url).origin === self.location.origin,
      );
      if (open) {
        await open.focus();
        await open.navigate(url).catch(() => undefined);
      } else {
        await self.clients.openWindow(url);
      }
    })(),
  );
});
