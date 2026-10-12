import { QueryCache, QueryClient, QueryKey } from '@tanstack/react-query';
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

localClient.interceptors.request.use((req) => {
  req.headers.set('X-Requested-With', 'JavaScript');
  return req;
});

const noRedirectUrls = ['/', '/error', '/dev/i18n', '/dev/404'];

/**
 * Validation errors are either plain text or a json list of messages
 */
function parseErrorBody(body: string): string {
  try {
    const parsed: unknown = JSON.parse(body);
    if (Array.isArray(parsed)) {
      return parsed.map(String).join('\n');
    }
  } catch {
    // plain text
  }
  return body;
}

localClient.interceptors.response.use(async (res) => {
  if (res.status == 400) {
    throw new BackendError(res.status, parseErrorBody(await res.text()));
  }

  // the session is missing or expired, the backend answers JS requests with 499 instead of redirecting
  if (res.status == 499) {
    if (noRedirectUrls.includes(location.pathname)) {
      throw new BackendError(401, 'Login required');
    }
    localStorage.setItem(
      'postLoginRedirect',
      location.pathname + location.search,
    );
    location.href = `${baseUrl}/api/auth/login`;
    // keep the request pending while the browser leaves, instead of flashing an error
    return new Promise<Response>(() => {});
  }

  return res;
});

export { localClient, baseUrl };

const queryClient = new QueryClient({
  queryCache: new QueryCache(),
  defaultOptions: {
    queries: {
      retry: false,
      refetchOnWindowFocus: false,
    },
  },
});

interface GeneratedKey {
  _id?: string;
  path?: { carId?: number; id?: number };
}

/**
 * Invalidates every query belonging to a car, plus the car list, the stats and the reminders
 */
export function invalidateCar(carId: number) {
  return queryClient.invalidateQueries({
    predicate: (query) => {
      const key = (query.queryKey as QueryKey)[0] as GeneratedKey | undefined;
      if (!key) return false;
      // queries over all cars (-1), reminders also depend on the odometer of bills
      if (
        key._id === 'getMyCars' ||
        key._id === 'getDashboardStats' ||
        key._id === 'getMonthlyStats' ||
        key._id === 'getReminders'
      ) {
        return true;
      }
      return key.path?.carId === carId || key.path?.id === carId;
    },
  });
}

export default queryClient;
