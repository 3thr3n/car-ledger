import { defineConfig } from '@hey-api/openapi-ts';

export default defineConfig({
  input: './public/openapi.json',
  output: 'src/generated',
  plugins: [
    '@hey-api/client-fetch',
    '@tanstack/react-query',
    {
      enums: 'typescript',
      name: '@hey-api/typescript',
    },
  ],
});
