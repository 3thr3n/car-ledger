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
 * waiting for the network. The data of the API is never cached.
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

// a new version waits until the user reloads (see registerServiceWorker)
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') self.skipWaiting();
});
clientsClaim();
