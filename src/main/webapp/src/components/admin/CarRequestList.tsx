import {
  Badge,
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  Stack,
  TextField,
  ToggleButton,
  ToggleButtonGroup,
  Typography,
} from '@mui/material';
import { keepPreviousData, useMutation, useQuery } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { AdminCarRequest, RequestState } from '@/generated';
import {
  approveCarRequestMutation,
  getAdminCarRequestsOptions,
  rejectCarRequestMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import NumberField from '@/components/bill/NumberField';
import useOpenCarRequests from '@/hooks/useOpenCarRequests';

/** the same limits as the backend */
const MAX_LIMIT = 100;
const MAX_TEXT = 500;

type Filter = 'open' | 'all';

/** refreshes the requests, the statistics and the accounts with their limits */
const refresh = () =>
  queryClient.invalidateQueries({
    predicate: (query) =>
      (
        (query.queryKey[0] as { _id?: string } | undefined)?._id ?? ''
      ).startsWith('getAdmin'),
  });

const stateColor = {
  [RequestState.OPEN]: 'info',
  [RequestState.APPROVED]: 'success',
  [RequestState.REJECTED]: 'default',
} as const;

/**
 * Requests of the users for more cars, the open ones to approve or reject
 */
export default function CarRequestList() {
  const { t } = useTranslation();
  const format = useFormat();
  const [filter, setFilter] = useState<Filter>('open');
  const [approving, setApproving] = useState<AdminCarRequest>();
  const [rejecting, setRejecting] = useState<AdminCarRequest>();

  const open = useOpenCarRequests();
  const { data, isLoading } = useQuery({
    ...getAdminCarRequestsOptions({
      client: localClient,
      query: {
        state: filter === 'open' ? RequestState.OPEN : undefined,
        size: 50,
      },
    }),
    placeholderData: keepPreviousData,
  });
  const requests = data?.data ?? [];

  return (
    <Section
      title={
        <Badge
          badgeContent={open}
          color="error"
          sx={{ pr: 1.5 }}
          slotProps={{ badge: { 'aria-label': t('admin.requests.open') } }}
        >
          {t('admin.requests.title')}
        </Badge>
      }
      subtitle={t('admin.requests.subtitle')}
      action={
        <ToggleButtonGroup
          size="small"
          exclusive
          value={filter}
          onChange={(_, value) => value && setFilter(value)}
        >
          <ToggleButton value="open" sx={{ px: 1.5 }}>
            {t('admin.requests.filterOpen')}
          </ToggleButton>
          <ToggleButton value="all" sx={{ px: 1.5 }}>
            {t('admin.requests.filterAll')}
          </ToggleButton>
        </ToggleButtonGroup>
      }
      sx={{ mb: 3 }}
    >
      {!isLoading && requests.length === 0 ? (
        <EmptyState compact title={t('admin.requests.empty')} />
      ) : (
        <Stack>
          {requests.map((request, index) => (
            <Fragment key={request.id}>
              {index > 0 && <Divider />}
              <Stack
                direction={{ xs: 'column', sm: 'row' }}
                spacing={1.5}
                sx={{ py: 1.5, alignItems: { sm: 'center' } }}
              >
                <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                  <Typography sx={{ fontWeight: 600 }}>
                    {t('admin.requests.line', {
                      name: request.account,
                      count: request.cars,
                    })}
                  </Typography>
                  <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                    {[
                      t('admin.requests.usage', {
                        cars: request.currentCars,
                        max: request.currentMaxCars,
                      }),
                      format.date(request.createdAt.slice(0, 10)),
                    ].join(' · ')}
                  </Typography>
                  {request.reason && (
                    <Typography
                      variant="body2"
                      sx={{ mt: 0.5, overflowWrap: 'anywhere' }}
                    >
                      “{request.reason}”
                    </Typography>
                  )}
                  {request.comment && (
                    <Typography
                      variant="body2"
                      sx={{ mt: 0.5, color: 'text.secondary' }}
                    >
                      {t('admin.requests.comment', {
                        comment: request.comment,
                      })}
                    </Typography>
                  )}
                </Box>
                {request.state === RequestState.OPEN ? (
                  <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
                    <Button
                      color="inherit"
                      size="small"
                      onClick={() => setRejecting(request)}
                    >
                      {t('admin.requests.reject')}
                    </Button>
                    <Button
                      variant="contained"
                      size="small"
                      onClick={() => setApproving(request)}
                    >
                      {t('admin.requests.approve')}
                    </Button>
                  </Stack>
                ) : (
                  <Chip
                    size="small"
                    variant="outlined"
                    color={stateColor[request.state]}
                    label={
                      request.state === RequestState.APPROVED
                        ? t('admin.requests.approvedTo', {
                            count: request.grantedMaxCars ?? 0,
                          })
                        : t(`admin.requests.states.${request.state}`)
                    }
                    sx={{ alignSelf: { xs: 'flex-start', sm: 'center' } }}
                  />
                )}
              </Stack>
            </Fragment>
          ))}
        </Stack>
      )}
      <Dialog
        open={approving !== undefined}
        onClose={() => setApproving(undefined)}
        maxWidth="xs"
        fullWidth
      >
        {approving && (
          <ApproveForm
            key={approving.id}
            request={approving}
            onClose={() => setApproving(undefined)}
          />
        )}
      </Dialog>
      <Dialog
        open={rejecting !== undefined}
        onClose={() => setRejecting(undefined)}
        maxWidth="xs"
        fullWidth
      >
        {rejecting && (
          <RejectForm
            key={rejecting.id}
            request={rejecting}
            onClose={() => setRejecting(undefined)}
          />
        )}
      </Dialog>
    </Section>
  );
}

function ApproveForm({
  request,
  onClose,
}: {
  request: AdminCarRequest;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [maxCars, setMaxCars] = useState<number | undefined>(
    Math.min(request.currentMaxCars + request.cars, MAX_LIMIT),
  );

  const approve = useMutation({
    ...approveCarRequestMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('admin.requests.approved'));
      onClose();
      return refresh();
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
  });

  const invalid =
    maxCars === undefined ||
    maxCars <= request.currentMaxCars ||
    maxCars > MAX_LIMIT;

  return (
    <>
      <DialogTitle>
        {t('admin.requests.approveTitle', { name: request.account })}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <NumberField
            label={t('admin.edit.maxCars')}
            value={maxCars}
            onChange={setMaxCars}
            decimalScale={0}
            max={MAX_LIMIT}
            helperText={t('admin.requests.approveHelp', {
              cars: request.currentCars,
              max: request.currentMaxCars,
            })}
          />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          disabled={invalid || approve.isPending}
          onClick={() =>
            approve.mutate({ path: { id: request.id }, body: { maxCars } })
          }
        >
          {t('admin.requests.approve')}
        </Button>
      </DialogActions>
    </>
  );
}

function RejectForm({
  request,
  onClose,
}: {
  request: AdminCarRequest;
  onClose: () => void;
}) {
  const { t } = useTranslation();
  const [comment, setComment] = useState('');

  const reject = useMutation({
    ...rejectCarRequestMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('admin.requests.rejected'));
      onClose();
      return refresh();
    },
    onError: (error) => toast.error(errorMessage(error, t('errors.backend'))),
  });

  return (
    <>
      <DialogTitle>
        {t('admin.requests.rejectTitle', { name: request.account })}
      </DialogTitle>
      <DialogContent>
        <Box sx={{ pt: 1 }}>
          <TextField
            label={t('admin.requests.commentLabel')}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            helperText={t('admin.requests.commentHelp')}
            multiline
            minRows={2}
            slotProps={{ htmlInput: { maxLength: MAX_TEXT } }}
          />
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          color="error"
          disabled={reject.isPending}
          onClick={() =>
            reject.mutate({
              path: { id: request.id },
              body: { comment: comment.trim() || undefined },
            })
          }
        >
          {t('admin.requests.reject')}
        </Button>
      </DialogActions>
    </>
  );
}
