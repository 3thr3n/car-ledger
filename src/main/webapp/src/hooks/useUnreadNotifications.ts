import { useQuery } from '@tanstack/react-query';
import { getInboxUnreadOptions } from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';

/**
 * How many notifications of the inbox are unread. Fetched again every minute
 * while the app is visible, and after the inbox changed.
 */
export default function useUnreadNotifications(): number {
  const { data } = useQuery({
    ...getInboxUnreadOptions({ client: localClient }),
    refetchInterval: 60 * 1000,
  });
  return data?.count ?? 0;
}

/** the inbox and its count changed, e.g. a notification was read */
export const invalidateInbox = () =>
  queryClient.invalidateQueries({
    predicate: (query) =>
      ['getInbox', 'getInboxUnread'].includes(
        (query.queryKey[0] as { _id?: string } | undefined)?._id ?? '',
      ),
  });
