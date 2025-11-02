import { QueryClient } from '@tanstack/react-query';
import { BackendError } from './BackendError';
import { Client, createClient } from '@/generated/client';

const isDev = process.env.NODE_ENV !== 'production';
const baseUrl = isDev ? 'http://localhost:8080' : '';

const localClient: Client = createClient({
  baseUrl,
  credentials: 'include',
  redirect: 'manual',
});

localClient.interceptors.error.use((_, res) => {
  throw new BackendError(res.status);
});

localClient.interceptors.response.use((x) => {
  if (x.type == 'opaqueredirect') {
    throw new BackendError(401);
  }
  return x;
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
