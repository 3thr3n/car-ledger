import { QueryClient } from '@tanstack/react-query';
import { BackendError } from './BackendError';
import { Client, createClient } from '@/generated/client';
import { toast } from 'react-toastify';

const isDev = process.env.NODE_ENV !== 'production';
const baseUrl = isDev ? 'http://localhost:8080' : '';

const localClient: Client = createClient({
  baseUrl,
  credentials: 'include',
  redirect: 'manual',
});

localClient.interceptors.error.use(async (_, res) => {
  if (res.status == 500) {
    toast.error('Server internal error, please retry later!');
  }

  throw new BackendError(res.status);
});

localClient.interceptors.response.use(async (res) => {
  if (res.status == 400 || res.status == 302) {
    throw new BackendError(res.status, await res.text());
  }

  if (res.type == 'opaqueredirect') {
    throw new BackendError(401, 'Login required');
  }
  return res;
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
