import {
  Box,
  Button,
  Card,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import PersonAddRounded from '@mui/icons-material/PersonAddRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { CarAccessEntry, CarAccessState } from '@/generated';
import {
  getCarAccessOptions,
  getCarAccessQueryKey,
  revokeCarAccessMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import AccessDialog from './AccessDialog';
import { hasEnded } from './period';
import usePeriodText from './usePeriodText';

/**
 * The accounts with access to the car and the invitations, only for its
 * owner: invite, change the role or the period, revoke
 */
export default function AccessSection({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const [inviting, setInviting] = useState(false);
  const [changing, setChanging] = useState<CarAccessEntry>();
  const [revoking, setRevoking] = useState<CarAccessEntry>();
  const { data: accesses } = useQuery(
    getCarAccessOptions({ client: localClient, path: { carId } }),
  );

  const revoke = useMutation({
    ...revokeCarAccessMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('access.revoked'));
      await queryClient.invalidateQueries({
        queryKey: getCarAccessQueryKey({ path: { carId } }),
      });
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  return (
    <Card sx={{ p: 3, mt: 3 }} data-testid="car-access">
      <Stack spacing={2}>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{
            alignItems: { sm: 'center' },
            justifyContent: 'space-between',
          }}
        >
          <Box>
            <Typography sx={{ fontWeight: 600 }}>
              {t('access.title')}
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {t('access.text')}
            </Typography>
          </Box>
          <Button
            variant="outlined"
            startIcon={<PersonAddRounded />}
            onClick={() => setInviting(true)}
            sx={{ flexShrink: 0 }}
          >
            {t('access.invite')}
          </Button>
        </Stack>
        {accesses && accesses.length > 0 && (
          <Box>
            {accesses.map((access, index) => (
              <Fragment key={access.id}>
                {index > 0 && <Divider />}
                <AccessRow
                  access={access}
                  onChange={() => setChanging(access)}
                  onRevoke={() => setRevoking(access)}
                  disabled={revoke.isPending}
                />
              </Fragment>
            ))}
          </Box>
        )}
      </Stack>
      <AccessDialog
        open={inviting || changing !== undefined}
        carId={carId}
        access={changing}
        onClose={() => {
          setInviting(false);
          setChanging(undefined);
        }}
      />
      <ConfirmDialog
        open={revoking !== undefined}
        title={t(
          revoking?.state === CarAccessState.ACCEPTED
            ? 'access.revokeTitle'
            : 'access.withdrawTitle',
          { name: revoking?.account ?? revoking?.invitee },
        )}
        text={t('access.revokeText')}
        confirmLabel={t(
          revoking?.state === CarAccessState.ACCEPTED
            ? 'access.revoke'
            : 'access.withdraw',
        )}
        onClose={() => setRevoking(undefined)}
        onConfirm={() => revoke.mutate({ path: { carId, id: revoking!.id } })}
      />
    </Card>
  );
}

function AccessRow({
  access,
  onChange,
  onRevoke,
  disabled,
}: {
  access: CarAccessEntry;
  onChange: () => void;
  onRevoke: () => void;
  disabled: boolean;
}) {
  const { t } = useTranslation();
  const periodText = usePeriodText();
  const state = hasEnded(access)
    ? 'ENDED'
    : access.account
      ? access.state
      : 'NO_ACCOUNT';

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={{ xs: 1, sm: 2 }}
      sx={{ alignItems: { sm: 'center' }, py: 1.5 }}
      data-testid="access"
    >
      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        <Stack
          direction="row"
          spacing={1}
          sx={{ alignItems: 'center', flexWrap: 'wrap', rowGap: 0.5 }}
        >
          <Typography
            sx={{
              fontWeight: 500,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
            }}
          >
            {access.account ?? access.invitee}
          </Typography>
          <Chip size="small" label={t(`car.roles.${access.role}`)} />
          {state !== CarAccessState.ACCEPTED && (
            <Chip
              size="small"
              variant="outlined"
              color={state === CarAccessState.DECLINED ? 'error' : 'default'}
              label={t(`access.states.${state}`)}
            />
          )}
        </Stack>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {periodText(access) || t('access.period.always')}
        </Typography>
      </Box>
      <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
        <Button size="small" onClick={onChange} disabled={disabled}>
          {t('access.change')}
        </Button>
        <Button
          color="error"
          size="small"
          onClick={onRevoke}
          disabled={disabled}
        >
          {access.state === CarAccessState.ACCEPTED
            ? t('access.revoke')
            : t('access.withdraw')}
        </Button>
      </Stack>
    </Stack>
  );
}
