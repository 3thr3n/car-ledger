import { Alert, Button, Stack } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { CarInvitation } from '@/generated';
import {
  acceptInvitationMutation,
  declineInvitationMutation,
  getInvitationsOptions,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import usePeriodText from './usePeriodText';

/** the queries that change with an answer: the invitations and the cars */
const ANSWERED = ['getInvitations', 'getMyCars'];

/**
 * The invitations of others to their cars, until the user accepts or declines
 * them
 */
export default function InvitationList() {
  const { t } = useTranslation();
  const { data: invitations } = useQuery(
    getInvitationsOptions({ client: localClient }),
  );

  const options = {
    onSettled: () =>
      queryClient.invalidateQueries({
        predicate: (query) =>
          ANSWERED.includes(
            (query.queryKey[0] as { _id?: string } | undefined)?._id ?? '',
          ),
      }),
    onError: (e: unknown) => toast.error(errorMessage(e, t('errors.backend'))),
  };
  const accept = useMutation({
    ...acceptInvitationMutation({ client: localClient }),
    ...options,
    onSuccess: () => toast.success(t('access.accepted')),
  });
  const decline = useMutation({
    ...declineInvitationMutation({ client: localClient }),
    ...options,
    onSuccess: () => toast.info(t('access.declined')),
  });
  const pending = accept.isPending || decline.isPending;

  if (!invitations?.length) return null;

  return (
    <Stack spacing={1.5} sx={{ mb: 3 }} data-testid="invitations">
      {invitations.map((invitation) => (
        <Alert
          key={invitation.id}
          severity="info"
          data-testid="invitation"
          action={
            <Stack direction="row" spacing={1}>
              <Button
                color="inherit"
                size="small"
                disabled={pending}
                onClick={() => decline.mutate({ path: { id: invitation.id } })}
              >
                {t('access.decline')}
              </Button>
              <Button
                variant="contained"
                size="small"
                disabled={pending}
                onClick={() => accept.mutate({ path: { id: invitation.id } })}
              >
                {t('access.accept')}
              </Button>
            </Stack>
          }
        >
          <InvitationText invitation={invitation} />
        </Alert>
      ))}
    </Stack>
  );
}

function InvitationText({ invitation }: { invitation: CarInvitation }) {
  const { t } = useTranslation();
  const period = usePeriodText()(invitation);

  return (
    <>
      {t('access.invitation', {
        owner: invitation.owner,
        car: invitation.car,
        role: t(`car.roles.${invitation.role}`),
      })}
      {period && ` ${period}`}
    </>
  );
}
