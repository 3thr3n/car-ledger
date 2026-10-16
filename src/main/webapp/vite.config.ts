/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tanstackRouter from '@tanstack/router-plugin/vite';
import path from 'path';
import yaml from '@modyfi/vite-plugin-yaml';
import { VitePWA } from 'vite-plugin-pwa';

export default defineConfig({
  plugins: [
    // before the react plugin; splits the components of all routes into own
    // chunks, also of the ones that are not .lazy files
    tanstackRouter({ target: 'react', autoCodeSplitting: true }),
    react(),
    yaml(),
    // installable app: manifest, icons and the service worker of src/sw, only
    // in the build (the dev server and the e2e tests run without it)
    VitePWA({
      strategies: 'injectManifest',
      srcDir: 'src/sw',
      filename: 'sw.ts',
      // registered in src/utils/serviceWorker.ts, a new version asks before reloading
      injectRegister: false,
      registerType: 'prompt',
      // .json: the server knows its content type, not the one of .webmanifest
      manifestFilename: 'manifest.json',
      injectManifest: {
        globPatterns: ['**/*.{js,css,html,svg,png,ico,woff2}'],
        // the spec of the API is no part of the app
        globIgnores: ['openapi.json'],
      },
      manifest: {
        name: 'CarLedger',
        short_name: 'CarLedger',
        description: 'Track fuel, maintenance and recurring costs of your cars',
        lang: 'en',
        start_url: '/dashboard',
        scope: '/',
        display: 'standalone',
        background_color: '#f5f6f8',
        theme_color: '#0f766e',
        icons: [
          { src: '/icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: '/icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: '/icons/maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
        // long press on the icon: the form of a new refuel (or charge) of the
        // last used car
        shortcuts: [
          {
            name: 'Add refuel',
            short_name: 'Refuel',
            url: '/refuel',
            icons: [
              {
                src: '/icons/icon-192.png',
                sizes: '192x192',
                type: 'image/png',
              },
            ],
          },
        ],
      },
    }),
  ],
  test: {
    // the playwright specs in e2e/ run separately
    include: ['src/**/*.test.{ts,tsx}'],
  },
  build: {
    rolldownOptions: {
      output: {
        // libraries the app always loads completely, in an own chunk that stays
        // cached between releases. Not @mui/material: parts of it are only used
        // by lazy routes and would end up in the first load.
        manualChunks: (id) =>
          /node_modules\/(react|react-dom|scheduler|@emotion\/[^/]+|@tanstack\/[^/]+|i18next[^/]*|react-i18next)\//.test(
            id,
          )
            ? 'vendor'
            : undefined,
      },
    },
  },
  resolve: {
    alias: {
      '@': path.resolve(import.meta.dirname, './src'),
      $: path.resolve(import.meta.dirname, './node_modules'),
    },
  },
});
