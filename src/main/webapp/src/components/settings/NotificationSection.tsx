import {
  Box,
  Button,
  Card,
  Divider,
  FormControlLabel,
  FormHelperText,
  Stack,
  Switch,
  Typography,
} from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { NotificationSettings } from '@/generated';
import {
  getMyNotificationsOptions,
  getMyNotificationsQueryKey,
  testMyPushMutation,
  updateMyNotificationsMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import useMe from '@/hooks/useMe';
import usePush from '@/hooks/usePush';

/**
 * Notifications by e-mail (to the address of the login) and as push messages
 * on the browsers that subscribed, and what they are about. The new requests
 * only for administrators.
 */
export default function NotificationSection() {
  const { t } = useTranslation();
  const { isAdmin } = useMe();
  const { data } = useQuery(getMyNotificationsOptions({ client: localClient }));
  const push = usePush();
  const [pushBusy, setPushBusy] = useState(false);

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

  const test = useMutation({
    ...testMyPushMutation({ client: localClient }),
    onSuccess: () => toast.success(t('notifications.push.testSent')),
    onError: (error) =>
      toast.error(errorMessage(error as unknown as Error, t('errors.backend'))),
  });

  if (!data) return null;

  const mailUsable = !!data.available && !!data.email;
  const mailHint = !data.available
    ? t('notifications.unavailable')
    : !data.email
      ? t('notifications.noEmail')
      : t('notifications.sentTo', { email: data.email });
  const pushUsable = push.state === 'on' || push.state === 'off';
  // the topics need a channel: mails, or a browser of the account
  const reachable = (mailUsable && !!data.byMail) || push.browsers > 0;

  const togglePush = async (on: boolean) => {
    setPushBusy(true);
    try {
      if (on) await push.subscribe();
      else await push.unsubscribe();
    } catch (error) {
      toast.error(errorMessage(error as Error, t('notifications.push.failed')));
    } finally {
      setPushBusy(false);
    }
  };

  const topic = (
    key: 'reminders' | 'requestDecided' | 'newRequests',
    label: string,
  ) => (
    <FormControlLabel
      control={
        <Switch
          checked={!!data[key]}
          disabled={update.isPending || !reachable}
          onChange={(_, checked) => update.mutate({ body: { [key]: checked } })}
        />
      }
      label={label}
    />
  );

  return (
    <Card sx={{ p: 3, mt: 3 }}>
      <Stack spacing={2}>
        <Box>
          <Typography sx={{ fontWeight: 600 }}>
            {t('notifications.title')}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('notifications.inbox')}
          </Typography>
        </Box>

        <Box>
          <FormControlLabel
            control={
              <Switch
                checked={mailUsable && !!data.byMail}
                disabled={update.isPending || !mailUsable}
                onChange={(_, checked) =>
                  update.mutate({ body: { byMail: checked } })
                }
              />
            }
            label={t('notifications.byMail')}
          />
          <FormHelperText sx={{ mt: 0 }}>{mailHint}</FormHelperText>
        </Box>

        <Box>
          <FormControlLabel
            control={
              <Switch
                checked={push.state === 'on'}
                disabled={pushBusy || !pushUsable}
                onChange={(_, checked) => togglePush(checked)}
              />
            }
            label={t('notifications.push.label')}
          />
          <FormHelperText sx={{ mt: 0 }} data-testid="push-hint">
            {pushBusy && push.state === 'off'
              ? t('notifications.push.turningOn')
              : t(`notifications.push.${push.state}`, { count: push.browsers })}
          </FormHelperText>
          {push.state === 'on' && (
            <Button
              size="small"
              sx={{ mt: 1 }}
              disabled={test.isPending}
              onClick={() => test.mutate({})}
            >
              {t('notifications.push.test')}
            </Button>
          )}
        </Box>

        <Divider />

        <Box>
          <Typography variant="body2" sx={{ color: 'text.secondary', mb: 1 }}>
            {reachable
              ? t('notifications.topics')
              : t('notifications.noChannel')}
          </Typography>
          <Stack spacing={1}>
            {topic('reminders', t('notifications.reminders'))}
            {topic('requestDecided', t('notifications.requestDecided'))}
            {isAdmin && topic('newRequests', t('notifications.newRequests'))}
          </Stack>
        </Box>
      </Stack>
    </Card>
  );
}
