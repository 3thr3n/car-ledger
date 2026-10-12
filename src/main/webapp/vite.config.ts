/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tanstackRouter from '@tanstack/router-plugin/vite';
import path from 'path';
import yaml from '@modyfi/vite-plugin-yaml';

export default defineConfig({
  plugins: [
    // before the react plugin; splits the components of all routes into own
    // chunks, also of the ones that are not .lazy files
    tanstackRouter({ target: 'react', autoCodeSplitting: true }),
    react(),
    yaml(),
  ],
  test: {
    // the playwright specs in e2e/ run separately
    include: ['src/**/*.test.{ts,tsx}'],
  },
  build: {
    rollupOptions: {
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
      '@': path.resolve(__dirname, './src'),
      $: path.resolve(__dirname, './node_modules'),
    },
  },
});
