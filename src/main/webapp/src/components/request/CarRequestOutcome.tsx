import { Alert } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { RequestState } from '@/generated';
import { markCarRequestSeenMutation } from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import useCarRequest from '@/hooks/useCarRequest';

/**
 * The decision about the own request for more cars, shown until it is closed
 */
export default function CarRequestOutcome() {
  const { t } = useTranslation();
  const { request, hasNews } = useCarRequest();

  const seen = useMutation({
    ...markCarRequestSeenMutation({ client: localClient }),
    onSettled: () =>
      queryClient.invalidateQueries({
        predicate: (query) =>
          (query.queryKey[0] as { _id?: string } | undefined)?._id ===
          'getMyLatestCarRequest',
      }),
  });

  if (!request || !hasNews) return null;
  const approved = request.state === RequestState.APPROVED;

  return (
    <Alert
      severity={approved ? 'success' : 'info'}
      variant="outlined"
      onClose={() => seen.mutate({ path: { id: request.id } })}
      sx={{ mb: 2.5 }}
    >
      {approved
        ? t('carRequest.approved', { count: request.grantedMaxCars ?? 0 })
        : request.comment
          ? t('carRequest.rejectedWithComment', { comment: request.comment })
          : t('carRequest.rejected')}
    </Alert>
  );
}
