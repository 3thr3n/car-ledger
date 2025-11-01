import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  client: '@hey-api/client-fetch',
  input: './public/openapi.json',
  output: 'src/generated',
  plugins: ['@tanstack/react-query'],
});
