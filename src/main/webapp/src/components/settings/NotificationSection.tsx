import {
  Box,
  Card,
  FormControlLabel,
  Stack,
  Switch,
  Typography,
} from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { NotificationSettings } from '@/generated';
import {
  getMyNotificationsOptions,
  getMyNotificationsQueryKey,
  updateMyNotificationsMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import useMe from '@/hooks/useMe';

/**
 * Notifications by e-mail: to the address of the login, each one can be turned
 * off. The one about new requests only for administrators.
 */
export default function NotificationSection() {
  const { t } = useTranslation();
  const { isAdmin } = useMe();
  const { data } = useQuery(getMyNotificationsOptions({ client: localClient }));

  const update = useMutation({
    ...updateMyNotificationsMutation({ client: localClient }),
    onSuccess: (settings: NotificationSettings) => {
      queryClient.setQueryData(
        getMyNotificationsQueryKey({ client: localClient }),
        settings,
      );
      toast.success(t('notifications.saved'));
    },
    onError: (error) =>
      toast.error(errorMessage(error as unknown as Error, t('errors.backend'))),
  });

  if (!data) return null;

  const usable = !!data.available && !!data.email;
  const hint = !data.available
    ? t('notifications.unavailable')
    : !data.email
      ? t('notifications.noEmail')
      : t('notifications.sentTo', { email: data.email });

  return (
    <Card sx={{ p: 3, mt: 3 }}>
      <Stack spacing={1.5}>
        <Box>
          <Typography sx={{ fontWeight: 600 }}>
            {t('notifications.title')}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {hint}
          </Typography>
        </Box>
        {usable && (
          <Stack>
            <FormControlLabel
              control={
                <Switch
                  checked={!!data.reminders}
                  disabled={update.isPending}
                  onChange={(_, checked) =>
                    update.mutate({ body: { reminders: checked } })
                  }
                />
              }
              label={t('notifications.reminders')}
            />
            <FormControlLabel
              control={
                <Switch
                  checked={!!data.requestDecided}
                  disabled={update.isPending}
                  onChange={(_, checked) =>
                    update.mutate({ body: { requestDecided: checked } })
                  }
                />
              }
              label={t('notifications.requestDecided')}
            />
            {isAdmin && (
              <FormControlLabel
                control={
                  <Switch
                    checked={!!data.newRequests}
                    disabled={update.isPending}
                    onChange={(_, checked) =>
                      update.mutate({ body: { newRequests: checked } })
                    }
                  />
                }
                label={t('notifications.newRequests')}
              />
            )}
          </Stack>
        )}
      </Stack>
    </Card>
  );
}
