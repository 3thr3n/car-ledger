import {
  Box,
  Button,
  Card,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import DoneAllRounded from '@mui/icons-material/DoneAllRounded';
import NotificationsNoneRounded from '@mui/icons-material/NotificationsNoneRounded';
import { useInfiniteQuery, useMutation } from '@tanstack/react-query';
import { useNavigate } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { InboxEntry } from '@/generated';
import {
  deleteInboxEntryMutation,
  getInboxInfiniteOptions,
  readInboxEntryMutation,
  readInboxMutation,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import { invalidateInbox } from '@/hooks/useUnreadNotifications';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import EmptyState from '@/components/ui/EmptyState';

const PAGE_SIZE = 20;

/**
 * The inbox: every notification of the server, also with mail and push off,
 * for 90 days. Opening one marks it as read.
 */
export default function NotificationsPage() {
  const { t } = useTranslation();
  const format = useFormat();
  const navigate = useNavigate();

  const inbox = useInfiniteQuery({
    ...getInboxInfiniteOptions({
      client: localClient,
      query: { size: PAGE_SIZE },
    }),
    initialPageParam: 1,
    getNextPageParam: (last) =>
      last.page * last.size < last.total ? last.page + 1 : undefined,
  });
  const entries = inbox.data?.pages.flatMap((page) => page.data) ?? [];
  const unread = inbox.data?.pages[0]?.unread ?? 0;

  const onError = (e: unknown) =>
    toast.error(errorMessage(e, t('errors.backend')));
  const read = useMutation({
    ...readInboxEntryMutation({ client: localClient }),
    onSuccess: invalidateInbox,
  });
  const readAll = useMutation({
    ...readInboxMutation({ client: localClient }),
    onSuccess: invalidateInbox,
    onError,
  });
  const remove = useMutation({
    ...deleteInboxEntryMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('inbox.deleted'));
      await invalidateInbox();
    },
    onError,
  });

  const open = (entry: InboxEntry) => {
    if (!entry.readAt) read.mutate({ path: { id: entry.id } });
    void navigate({ to: entry.path });
  };

  return (
    <Page id="NotificationsPage" maxWidth="md">
      <PageHeader
        title={t('inbox.title')}
        subtitle={t('inbox.subtitle')}
        actions={
          <Button
            variant="outlined"
            startIcon={<DoneAllRounded />}
            onClick={() => readAll.mutate({})}
            disabled={unread === 0 || readAll.isPending}
          >
            {t('inbox.readAll')}
          </Button>
        }
      />
      <Card>
        {inbox.isSuccess && entries.length === 0 ? (
          <EmptyState
            icon={<NotificationsNoneRounded />}
            title={t('inbox.empty')}
            text={t('inbox.emptyText')}
          />
        ) : (
          <List disablePadding data-testid="inbox">
            {entries.map((entry) => (
              <ListItem
                key={entry.id}
                disablePadding
                divider
                data-testid="inbox-entry"
                data-unread={!entry.readAt}
                secondaryAction={
                  <Tooltip title={t('inbox.delete')}>
                    <IconButton
                      edge="end"
                      aria-label={t('inbox.deleteLabel', {
                        title: entry.title,
                      })}
                      onClick={() => remove.mutate({ path: { id: entry.id } })}
                      disabled={remove.isPending}
                    >
                      <DeleteOutlineRounded />
                    </IconButton>
                  </Tooltip>
                }
              >
                <ListItemButton
                  onClick={() => open(entry)}
                  sx={{ alignItems: 'flex-start', gap: 1.5, py: 1.5 }}
                >
                  <Box
                    aria-hidden
                    sx={{
                      mt: 0.9,
                      width: 8,
                      height: 8,
                      flexShrink: 0,
                      borderRadius: '50%',
                      bgcolor: entry.readAt ? 'transparent' : 'primary.main',
                    }}
                  />
                  <ListItemText
                    primary={
                      <>
                        {!entry.readAt && (
                          <Box component="span" sx={visuallyHidden}>
                            {t('inbox.unread')}:{' '}
                          </Box>
                        )}
                        {entry.title}
                      </>
                    }
                    secondary={
                      <>
                        {entry.text && (
                          <Typography
                            component="span"
                            variant="body2"
                            sx={{ display: 'block', whiteSpace: 'pre-line' }}
                          >
                            {entry.text}
                          </Typography>
                        )}
                        <Typography
                          component="span"
                          variant="caption"
                          sx={{ color: 'text.secondary' }}
                        >
                          {format.dateTime(entry.createdAt)}
                        </Typography>
                      </>
                    }
                    slotProps={{
                      primary: {
                        sx: { fontWeight: entry.readAt ? 400 : 600 },
                      },
                    }}
                  />
                </ListItemButton>
              </ListItem>
            ))}
          </List>
        )}
        {inbox.hasNextPage && (
          <Box sx={{ p: 2, textAlign: 'center' }}>
            <Button
              onClick={() => inbox.fetchNextPage()}
              disabled={inbox.isFetchingNextPage}
            >
              {t('inbox.more')}
            </Button>
          </Box>
        )}
      </Card>
    </Page>
  );
}

/** read by screen readers only */
const visuallyHidden = {
  position: 'absolute',
  width: 1,
  height: 1,
  overflow: 'hidden',
  clip: 'rect(0 0 0 0)',
  whiteSpace: 'nowrap',
} as const;
