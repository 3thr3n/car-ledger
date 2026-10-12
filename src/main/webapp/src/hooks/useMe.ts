import { useQuery } from '@tanstack/react-query';
import { getMyselfOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

/**
 * The logged-in account. `loggedIn` is false while loading or when the session is missing.
 */
export default function useMe() {
  const { data, isLoading } = useQuery({
    ...getMyselfOptions({ client: localClient }),
    refetchInterval: 5 * 60 * 1000,
    refetchIntervalInBackground: true,
  });

  return {
    account: data,
    loggedIn: !!data,
    isLoading,
    maxCars: data?.maxCars ?? 0,
    isAdmin: data?.admin ?? false,
  };
}
