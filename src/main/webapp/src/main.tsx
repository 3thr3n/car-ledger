import React, { StrictMode } from 'react';
import ReactDOM from 'react-dom/client';
import { createRouter, RouterProvider } from '@tanstack/react-router';

import '@fontsource-variable/inter';
import './styles/index.css';

// Import the generated route tree
import { routeTree } from './routeTree.gen';

// Import internationalization
import './i18n';
import { registerServiceWorker } from '@/utils/serviceWorker';
import { listenForInstallPrompt } from '@/utils/install';
import queryClient from '@/utils/QueryClient';
import { offlineQueue, offlineStore } from '@/offline';
import { keepQueries, restoreQueries } from '@/offline/persist';

// Create a new router instance
const router = createRouter({ routeTree });

// Register the router instance for type safety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router;
  }
}

registerServiceWorker();
listenForInstallPrompt();
// the queue and the data of the forms of the last visit, for offline
keepQueries(queryClient, offlineStore);
void offlineQueue.load();

// Render the app
const rootElement = document.getElementById('root')!;
if (!rootElement.innerHTML) {
  void restoreQueries(queryClient, offlineStore).finally(() => {
    const root = ReactDOM.createRoot(rootElement);
    root.render(
      <StrictMode>
        <RouterProvider router={router} />
      </StrictMode>,
    );
  });
}
