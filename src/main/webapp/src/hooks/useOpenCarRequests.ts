import { useQuery } from '@tanstack/react-query';
import { RequestState } from '@/generated';
import { getAdminCarRequestsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useMe from '@/hooks/useMe';

/**
 * Number of the open requests for more cars, 0 for users without the role
 * admin. Fetched again every minute while the app is visible, and when the
 * administration page decides a request (same query).
 */
export default function useOpenCarRequests() {
  const { isAdmin } = useMe();
  const { data } = useQuery({
    ...getAdminCarRequestsOptions({
      client: localClient,
      query: { state: RequestState.OPEN, size: 1 },
    }),
    enabled: isAdmin,
    refetchInterval: 60 * 1000,
  });

  return isAdmin ? (data?.total ?? 0) : 0;
}
