import { useQuery } from '@tanstack/react-query';
import { getInvitationsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

/**
 * The invitations to the cars of others the user hasn't answered yet. Fetched
 * again every minute while the app is visible, and when the garage answers one
 * (same query).
 */
export default function useOpenInvitations() {
  const { data } = useQuery({
    ...getInvitationsOptions({ client: localClient }),
    refetchInterval: 60 * 1000,
  });
  return data ?? [];
}
