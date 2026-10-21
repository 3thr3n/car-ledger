import {
  Box,
  Button,
  Card,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Device } from '@/generated';
import {
  getTrackersOptions,
  getTrackersQueryKey,
  revokeTrackerMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import TrackerAddDialog from '@/components/car/TrackerAddDialog';

/**
 * The GPS trackers of a car: each sends its points with its own token, the
 * owner creates and revokes them
 */
export default function TrackerSection({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const [adding, setAdding] = useState(false);
  const [revoking, setRevoking] = useState<Device>();
  const { data: trackers } = useQuery(
    getTrackersOptions({ client: localClient, path: { carId } }),
  );

  const revoke = useMutation({
    ...revokeTrackerMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('tracker.revoked'));
      await queryClient.invalidateQueries({
        queryKey: getTrackersQueryKey({ path: { carId } }),
      });
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  return (
    <Card sx={{ p: 3, mt: 3 }} data-testid="trackers">
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
              {t('tracker.title')}
            </Typography>
            <Typography variant="body2" sx={{ color: 'text.secondary' }}>
              {t('tracker.text')}
            </Typography>
          </Box>
          <Button
            variant="outlined"
            startIcon={<AddRounded />}
            onClick={() => setAdding(true)}
            sx={{ flexShrink: 0 }}
          >
            {t('tracker.add')}
          </Button>
        </Stack>
        {trackers && trackers.length > 0 && (
          <Box>
            {trackers.map((tracker, index) => (
              <Fragment key={tracker.id}>
                {index > 0 && <Divider />}
                <TrackerRow
                  tracker={tracker}
                  onRevoke={() => setRevoking(tracker)}
                  disabled={revoke.isPending}
                />
              </Fragment>
            ))}
          </Box>
        )}
      </Stack>
      <TrackerAddDialog
        open={adding}
        carId={carId}
        onClose={() => setAdding(false)}
      />
      <ConfirmDialog
        open={revoking !== undefined}
        title={t('tracker.revokeTitle', { name: revoking?.name })}
        text={t('tracker.revokeText')}
        confirmLabel={t('tracker.revoke')}
        onClose={() => setRevoking(undefined)}
        onConfirm={() => revoke.mutate({ path: { carId, id: revoking!.id } })}
      />
    </Card>
  );
}

function TrackerRow({
  tracker,
  onRevoke,
  disabled,
}: {
  tracker: Device;
  onRevoke: () => void;
  disabled: boolean;
}) {
  const { t } = useTranslation();
  const format = useFormat();

  return (
    <Stack
      direction="row"
      spacing={2}
      sx={{ alignItems: 'center', py: 1.5 }}
      data-testid="tracker"
    >
      <Box sx={{ flexGrow: 1, minWidth: 0 }}>
        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Typography
            sx={{
              fontWeight: 500,
              overflow: 'hidden',
              textOverflow: 'ellipsis',
              whiteSpace: 'nowrap',
              color: tracker.revokedAt ? 'text.secondary' : undefined,
            }}
          >
            {tracker.name}
          </Typography>
          {tracker.revokedAt && (
            <Chip size="small" label={t('tracker.revokedChip')} />
          )}
        </Stack>
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {tracker.revokedAt
            ? t('tracker.revokedOn', { date: format.date(tracker.revokedAt) })
            : tracker.lastSeenAt
              ? t('tracker.lastSeen', {
                  date: format.dateTime(tracker.lastSeenAt),
                })
              : t('tracker.neverSeen')}
          {' · '}
          {t('tracker.createdOn', { date: format.date(tracker.createdAt) })}
        </Typography>
      </Box>
      {!tracker.revokedAt && (
        <Button
          color="error"
          size="small"
          onClick={onRevoke}
          disabled={disabled}
          sx={{ flexShrink: 0 }}
        >
          {t('tracker.revoke')}
        </Button>
      )}
    </Stack>
  );
}
