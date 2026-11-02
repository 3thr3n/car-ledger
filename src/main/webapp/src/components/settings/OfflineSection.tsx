import { Box, Button, Stack } from '@mui/material';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import Section from '@/components/ui/Section';
import { offlineQueue } from '@/offline';
import { useOthersQueued } from '@/offline/useQueued';
import ConfirmDialog from '@/components/ui/ConfirmDialog';

/**
 * On a device used by several accounts: the entries another account made
 * offline wait for its login, they can be discarded here
 */
export default function OfflineSection() {
  const { t } = useTranslation();
  const others = useOthersQueued();
  const [open, setOpen] = useState(false);

  if (others.length === 0) return null;

  return (
    <Section
      title={t('offline.others.title')}
      subtitle={t('offline.others.text', { count: others.length })}
      sx={{ mb: 3 }}
    >
      <Stack spacing={1.5} data-testid="offline-others">
        <Box>
          <Button
            variant="outlined"
            color="error"
            onClick={() => setOpen(true)}
          >
            {t('offline.list.discard')}
          </Button>
        </Box>
      </Stack>
      <ConfirmDialog
        open={open}
        title={t('offline.others.discardTitle', { count: others.length })}
        text={t('offline.list.discardText')}
        confirmLabel={t('offline.list.discard')}
        onClose={() => setOpen(false)}
        onConfirm={() =>
          others.forEach((entry) => void offlineQueue.discard(entry.id))
        }
      />
    </Section>
  );
}
