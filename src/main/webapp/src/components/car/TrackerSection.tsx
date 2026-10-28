import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  Divider,
  Stack,
  Typography,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import UsbRounded from '@mui/icons-material/UsbRounded';
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
import TrackHistory from '@/components/car/TrackHistory';
import LazyTrackerFlashDialog from '@/components/tracker/LazyTrackerFlashDialog';
import useFirmware from '@/components/tracker/useFirmware';
import TrackerStatusDetails from '@/components/car/TrackerStatusDetails';

/**
 * The GPS trackers of a car: each sends its points with its own token, the
 * owner creates and revokes them, and flashes them over USB when the server has
 * the firmware
 */
export default function TrackerSection({
  carId,
  retentionDays,
}: {
  carId: number;
  /** of the track points, forever without */
  retentionDays?: number | null;
}) {
  const { t } = useTranslation();
  const [adding, setAdding] = useState(false);
  const [revoking, setRevoking] = useState<Device>();
  const [flashing, setFlashing] = useState<{
    tracker: Device;
    token?: string;
  }>();
  const firmware = useFirmware();
  const canFlash = !!firmware.version && firmware.serial;
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
        {firmware.version && !firmware.serial && (
          <Alert severity="info">{t('tracker.flash.noSerial')}</Alert>
        )}
        {trackers && trackers.length > 0 && (
          <Box>
            {trackers.map((tracker, index) => (
              <Fragment key={tracker.id}>
                {index > 0 && <Divider />}
                <TrackerRow
                  tracker={tracker}
                  offered={firmware.version}
                  onFlash={
                    canFlash ? () => setFlashing({ tracker }) : undefined
                  }
                  onRevoke={() => setRevoking(tracker)}
                  disabled={revoke.isPending}
                />
              </Fragment>
            ))}
          </Box>
        )}
        {trackers && trackers.length > 0 && (
          <>
            <Divider />
            <TrackHistory carId={carId} retentionDays={retentionDays} />
          </>
        )}
      </Stack>
      <TrackerAddDialog
        open={adding}
        carId={carId}
        onClose={() => setAdding(false)}
        onFlash={
          canFlash
            ? ({ device, token }) => {
                setAdding(false);
                setFlashing({ tracker: device, token });
              }
            : undefined
        }
      />
      {flashing && (
        <LazyTrackerFlashDialog
          open
          carId={carId}
          tracker={flashing.tracker}
          token={flashing.token}
          onClose={() => setFlashing(undefined)}
        />
      )}
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
  offered,
  onFlash,
  onRevoke,
  disabled,
}: {
  tracker: Device;
  /** version of the firmware the server offers for flashing */
  offered?: string;
  onFlash?: () => void;
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
        {tracker.status && !tracker.revokedAt && (
          <TrackerStatusDetails
            status={tracker.status}
            statusAt={tracker.statusAt}
            offered={offered}
          />
        )}
      </Box>
      {!tracker.revokedAt && onFlash && (
        <Button
          size="small"
          startIcon={<UsbRounded />}
          onClick={onFlash}
          sx={{ flexShrink: 0 }}
        >
          {t('tracker.flash.open')}
        </Button>
      )}
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
