import { useMutation } from '@tanstack/react-query';
import { Account, SettingsInput } from '@/generated';
import {
  getMyselfQueryKey,
  updateMySettingsMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';

/**
 * Saves the settings of the own account and updates the cached account
 */
export default function useSaveSettings(
  options: { onSuccess?: () => void; onError?: (error: Error) => void } = {},
) {
  const mutation = useMutation({
    ...updateMySettingsMutation({ client: localClient }),
    onSuccess: (account: Account) => {
      queryClient.setQueryData(
        getMyselfQueryKey({ client: localClient }),
        account,
      );
      options.onSuccess?.();
    },
    // the client throws a BackendError, not the typed response body
    onError: (error) => options.onError?.(error as unknown as Error),
  });

  return {
    save: (settings: SettingsInput) => mutation.mutate({ body: settings }),
    isPending: mutation.isPending,
  };
}
