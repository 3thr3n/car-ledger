import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import tanstackRouter from '@tanstack/router-plugin/vite';
import path from 'path';
import yaml from '@modyfi/vite-plugin-yaml';

export default defineConfig({
  plugins: [react(), tanstackRouter(), yaml()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },
});
