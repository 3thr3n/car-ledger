/// <reference types="vitest/config" />
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tanstackRouter from '@tanstack/router-plugin/vite';
import path from 'path';
import yaml from '@modyfi/vite-plugin-yaml';

export default defineConfig({
  plugins: [react(), tanstackRouter(), yaml()],
  test: {
    // the playwright specs in e2e/ run separately
    include: ['src/**/*.test.{ts,tsx}'],
  },
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
      $: path.resolve(__dirname, './node_modules'),
    },
  },
});
