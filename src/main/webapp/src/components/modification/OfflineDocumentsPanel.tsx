import {
  Box,
  FormControlLabel,
  Stack,
  Switch,
  Typography,
} from '@mui/material';
import OfflinePinRounded from '@mui/icons-material/OfflinePinRounded';
import { useQuery } from '@tanstack/react-query';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { useFormat } from '@/utils/format';
import { MAX_OFFLINE_BYTES } from '@/offline/documents';
import useOfflineDocuments, { installedOptions } from './useOfflineDocuments';

/**
 * Keeps the documents of the installed parts on the device. While it is on,
 * they are saved again whenever the list of the parts was loaded, so new,
 * deleted and removed ones follow.
 */
export default function OfflineDocumentsPanel({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const { available, kept, save, drop } = useOfflineDocuments(carId);
  const { dataUpdatedAt } = useQuery(installedOptions(carId));
  const on = !!kept;
  const { mutate: saveNow, isPending: saving } = save;

  useEffect(() => {
    if (on && dataUpdatedAt && navigator.onLine && !saving) saveNow();
    // only when the list changed, not when the saving ends
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [on, dataUpdatedAt]);

  if (!available) return null;

  const toggle = (checked: boolean) => {
    if (checked) {
      save.mutate(undefined, {
        onError: () => toast.error(t('modification.offline.failed')),
      });
    } else {
      drop.mutate();
    }
  };

  return (
    <Box
      data-testid="offline-documents"
      sx={{
        p: 2,
        borderRadius: 2,
        border: 1,
        borderColor: 'divider',
      }}
    >
      <Stack direction="row" spacing={1.5} sx={{ alignItems: 'flex-start' }}>
        <OfflinePinRounded
          color={on ? 'primary' : 'disabled'}
          sx={{ mt: 0.75 }}
        />
        <Box sx={{ flexGrow: 1, minWidth: 0 }}>
          <FormControlLabel
            control={
              <Switch
                // the new state at once, while it is saved or dropped
                checked={!drop.isPending && (on || saving)}
                disabled={saving || drop.isPending}
                onChange={(event) => toggle(event.target.checked)}
              />
            }
            label={t('modification.offline.label')}
            labelPlacement="start"
            sx={{ ml: 0, width: '100%', justifyContent: 'space-between' }}
          />
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {saving
              ? t('modification.offline.saving')
              : kept
                ? t('modification.offline.saved', {
                    time: format.dateTime(kept.savedAt),
                    count: kept.urls.length,
                  })
                : t('modification.offline.text')}
          </Typography>
          {kept && kept.skipped.length > 0 && (
            <Typography variant="body2" color="warning" sx={{ mt: 0.5 }}>
              {t('modification.offline.skipped', {
                count: kept.skipped.length,
                size: `${MAX_OFFLINE_BYTES / 1024 / 1024} MB`,
                names: kept.skipped.join(', '),
              })}
            </Typography>
          )}
        </Box>
      </Stack>
    </Box>
  );
}
