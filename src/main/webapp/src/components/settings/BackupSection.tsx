import { Box, Button, Card, Stack, Typography } from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import RestoreRounded from '@mui/icons-material/RestoreRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ChangeEvent, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  getMyCarsOptions,
  restoreBackupMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { baseUrl, localClient } from '@/utils/QueryClient';
import { BackendError, errorMessage } from '@/utils/BackendError';

/** where the browser downloads the backup */
export const backupUrl = `${baseUrl}/api/user/me/backup`;

/**
 * Downloads all own data as ZIP, and restores such a file into an account
 * without cars
 */
export default function BackupSection() {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));
  const canRestore = cars !== undefined && cars.length === 0;

  const restore = useMutation({
    ...restoreBackupMutation({ client: localClient }),
    onSuccess: (summary) => {
      toast.success(
        t('backup.restored', {
          cars: summary.cars,
          bills: summary.bills,
          files: summary.files,
        }),
      );
      // the settings, the cars and everything below them
      return queryClient.invalidateQueries();
    },
    onError: (error) =>
      toast.error(
        // the limit of the server or of a proxy in front of it
        error instanceof BackendError && error.status === 413
          ? t('backup.tooLarge')
          : errorMessage(error, t('backup.failed')),
      ),
  });

  const pick = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // the same file can be picked again
    event.target.value = '';
    if (file) restore.mutate({ body: { file } });
  };

  return (
    <Card sx={{ p: 3, mt: 3 }}>
      <Stack spacing={2}>
        <Box>
          <Typography sx={{ fontWeight: 600 }}>{t('backup.title')}</Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('backup.text')}
          </Typography>
        </Box>
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1}>
          <Button
            href={backupUrl}
            variant="outlined"
            startIcon={<FileDownloadRounded />}
          >
            {t('backup.download')}
          </Button>
          <Button
            variant="outlined"
            color="inherit"
            startIcon={<RestoreRounded />}
            disabled={!canRestore || restore.isPending}
            onClick={() => input.current?.click()}
          >
            {t('backup.restore')}
          </Button>
        </Stack>
        <Typography variant="caption" sx={{ color: 'text.secondary' }}>
          {t(canRestore ? 'backup.restoreHelp' : 'backup.restoreOnlyEmpty')}
        </Typography>
      </Stack>
      <input
        ref={input}
        type="file"
        accept=".zip,application/zip"
        hidden
        onChange={pick}
        data-testid="backup-input"
      />
    </Card>
  );
}
