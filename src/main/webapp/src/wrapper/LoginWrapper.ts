import { Credentials } from '@/generated';
import { loginOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { QueryKey, useMutation } from '@tanstack/react-query';

export function useLoginMutation() {
  return useMutation({
    mutationFn: async (body: Credentials) => {
      const { queryFn, queryKey, meta } = loginOptions({
        client: localClient,
        body,
      });
      if (queryFn && queryKey) {
        return queryFn({
          queryKey: queryKey[0] as QueryKey,
          signal: new AbortController().signal,
          meta: meta,
        });
      }
    },
  });
}
