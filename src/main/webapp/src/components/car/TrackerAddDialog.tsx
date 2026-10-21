import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  IconButton,
  InputAdornment,
  Stack,
  TextField,
  Tooltip,
} from '@mui/material';
import ContentCopyRounded from '@mui/icons-material/ContentCopyRounded';
import { useMutation } from '@tanstack/react-query';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  addTrackerMutation,
  getTrackersQueryKey,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';

export interface TrackerAddDialogProps {
  open: boolean;
  carId: number;
  onClose: () => void;
}

/**
 * Creates a GPS tracker and shows its token, which the server returns only
 * once
 */
export default function TrackerAddDialog({
  open,
  carId,
  onClose,
}: TrackerAddDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      {/* the form starts fresh, the token is gone after closing */}
      {open && <TrackerAddForm carId={carId} onClose={onClose} />}
    </Dialog>
  );
}

function TrackerAddForm({
  carId,
  onClose,
}: Omit<TrackerAddDialogProps, 'open'>) {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [touched, setTouched] = useState(false);
  const [token, setToken] = useState<string>();

  const error = !name.trim() ? t('tracker.errors.name') : undefined;

  const add = useMutation({
    ...addTrackerMutation({ client: localClient }),
    onSuccess: async (created) => {
      setToken(created.token);
      await queryClient.invalidateQueries({
        queryKey: getTrackersQueryKey({ path: { carId } }),
      });
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (error) return;
    add.mutate({ path: { carId }, body: { name: name.trim() } });
  };

  const copy = async () => {
    try {
      await navigator.clipboard.writeText(token!);
      toast.success(t('tracker.copied'));
    } catch {
      toast.error(t('tracker.copyFailed'));
    }
  };

  if (token) {
    return (
      <>
        <DialogTitle>{t('tracker.tokenTitle')}</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ pt: 1 }}>
            <Alert severity="warning">{t('tracker.tokenOnce')}</Alert>
            <TextField
              label={t('tracker.token')}
              value={token}
              slotProps={{
                input: {
                  readOnly: true,
                  sx: { fontFamily: 'monospace' },
                  endAdornment: (
                    <InputAdornment position="end">
                      <Tooltip title={t('tracker.copy')}>
                        <IconButton
                          aria-label={t('tracker.copy')}
                          onClick={copy}
                          edge="end"
                        >
                          <ContentCopyRounded />
                        </IconButton>
                      </Tooltip>
                    </InputAdornment>
                  ),
                },
                htmlInput: { 'data-testid': 'tracker-token' },
              }}
              onFocus={(event) => event.target.select()}
            />
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button variant="contained" onClick={onClose}>
            {t('tracker.done')}
          </Button>
        </DialogActions>
      </>
    );
  }

  return (
    <form onSubmit={submit} noValidate>
      <DialogTitle>{t('tracker.addTitle')}</DialogTitle>
      <DialogContent>
        <Stack sx={{ pt: 1 }}>
          <TextField
            autoFocus
            label={t('tracker.name')}
            placeholder={t('tracker.namePlaceholder')}
            value={name}
            onChange={(e) => setName(e.target.value)}
            error={touched && !!error}
            helperText={(touched && error) || t('tracker.nameHelp')}
            slotProps={{ htmlInput: { maxLength: 100 } }}
            required
          />
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button color="inherit" onClick={onClose}>
          {t('button.cancel')}
        </Button>
        <Button type="submit" variant="contained" disabled={add.isPending}>
          {t('tracker.create')}
        </Button>
      </DialogActions>
    </form>
  );
}
