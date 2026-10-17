import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import useMe from '@/hooks/useMe';
import { invalidateCar } from '@/utils/QueryClient';
import { offlineQueue } from '.';

/** a request the server could not take (e.g. 503) is sent again this often */
const RETRY = 60_000;

/**
 * Sends the queue of the logged-in user: at the start, when the device is
 * online again, when the app comes back to the foreground and every minute.
 */
export default function OfflineSync() {
  const { t } = useTranslation();
  const { account } = useMe();
  const user = account?.name;

  useEffect(() => {
    if (!user) return;
    const flush = async () => {
      if (!navigator.onLine) return;
      const { sent, failed } = await offlineQueue.flush(user);
      if (sent.length) {
        toast.success(t('offline.sent', { count: sent.length }));
        for (const carId of new Set(sent.map((entry) => entry.carId))) {
          await invalidateCar(carId);
        }
      }
      if (failed.length) {
        toast.error(t('offline.failed', { count: failed.length }));
      }
    };
    const onVisible = () => {
      if (document.visibilityState === 'visible') void flush();
    };

    void flush();
    window.addEventListener('online', flush);
    document.addEventListener('visibilitychange', onVisible);
    const timer = setInterval(flush, RETRY);
    return () => {
      window.removeEventListener('online', flush);
      document.removeEventListener('visibilitychange', onVisible);
      clearInterval(timer);
    };
  }, [user, t]);

  return null;
}
