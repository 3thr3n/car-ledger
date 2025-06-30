import { createClient } from '@hey-api/client-fetch';
import { QueryClient } from '@tanstack/react-query';
import { BackendError } from './BackendError';

const isDev = process.env.NODE_ENV !== 'production';
const baseUrl = isDev ? 'http://localhost:8080' : '';

const localClient = createClient({
  baseUrl,
  credentials: 'include',
});


let redirecting = false;
localClient.interceptors.response.use((response) => {
  if (response.status === 401 && !redirecting) {
    redirecting = true;

    setTimeout(() => {
      redirecting = false;
    }, 1000); // 1 second debounce window

    if (!window.location.pathname.startsWith('/login')) {
      window.location.href = '/login';
    }
  }

  return response;
})

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
