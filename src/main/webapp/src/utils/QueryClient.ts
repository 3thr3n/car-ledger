import { QueryClient } from '@tanstack/react-query';
import { BackendError } from './BackendError';
import { Client, createClient } from '@/generated/client';

const isDev = process.env.NODE_ENV !== 'production';
const baseUrl = isDev ? 'http://localhost:8080' : '';

const localClient: Client = createClient({
  baseUrl,
  credentials: 'include',
});

localClient.interceptors.error.use((_, res) => {
  throw new BackendError(res.status);
});

export { localClient, baseUrl };

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: false,
    },
  },
});
export default queryClient;
