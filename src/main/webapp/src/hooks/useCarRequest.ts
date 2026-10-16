import { useQuery } from '@tanstack/react-query';
import { CarRequest, RequestState } from '@/generated';
import { getMyLatestCarRequestOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

/**
 * The newest own request for more cars, undefined without one
 */
export default function useCarRequest() {
  const { data, isLoading } = useQuery(
    getMyLatestCarRequestOptions({ client: localClient }),
  );
  // the API answers 204 without a request
  const request = (data as CarRequest | undefined)?.id ? data : undefined;

  return {
    request,
    isLoading,
    isOpen: request?.state === RequestState.OPEN,
    /** decided, and the user has not seen it yet */
    hasNews:
      !!request && request.state !== RequestState.OPEN && !request.outcomeSeen,
  };
}
