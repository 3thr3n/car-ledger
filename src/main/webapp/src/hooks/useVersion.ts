import { useQuery } from '@tanstack/react-query';
import { getVersionOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import useMe from '@/hooks/useMe';

/** version of this build of the app, "dev" for local builds (like the server) */
export const APP_VERSION = import.meta.env.VITE_APP_VERSION || 'dev';

/**
 * The version of the app and of the server. An installed app may still run an
 * older version from its cache: then it is outdated.
 */
export default function useVersion() {
  const { loggedIn } = useMe();
  const { data } = useQuery({
    ...getVersionOptions({ client: localClient }),
    enabled: loggedIn,
    refetchInterval: 60 * 60 * 1000,
    refetchOnWindowFocus: true,
  });
  const server = data?.version;

  return {
    app: APP_VERSION,
    server,
    outdated: !!server && server !== APP_VERSION,
  };
}
