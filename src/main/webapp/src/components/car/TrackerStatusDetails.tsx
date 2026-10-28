import { Alert, Stack, Typography } from '@mui/material';
import { useTranslation } from 'react-i18next';
import type { TrackerStatus } from '@/generated';
import { fileSize, useFormat } from '@/utils/format';
import { storagePercent, trackerWarnings } from './trackerStatus';

/**
 * The latest status the tracker reported, with what needs a look first
 */
export default function TrackerStatusDetails({
  status,
  statusAt,
  offered,
}: {
  status: TrackerStatus;
  statusAt?: string;
  /** version of the firmware the server offers for flashing */
  offered?: string;
}) {
  const { t, i18n } = useTranslation();
  const format = useFormat();
  const percent = storagePercent(status);

  const facts = [
    status.firmware &&
      t('tracker.status.firmware', { version: status.firmware }),
    percent !== undefined &&
      t('tracker.status.storage', {
        percent,
        total: fileSize(i18n.language, status.storageTotal!),
      }),
    status.uploadResult &&
      t('tracker.status.upload', {
        result: status.uploadStatus
          ? `${status.uploadResult} (${status.uploadStatus})`
          : status.uploadResult,
      }),
    status.resetReason &&
      t('tracker.status.reset', { reason: status.resetReason }),
    status.ssid &&
      t('tracker.status.wifi', {
        ssid: status.ssid,
        rssi: status.rssi !== undefined ? ` (${status.rssi} dBm)` : '',
      }),
    status.parts?.length &&
      t('tracker.status.parts', { parts: status.parts.join(', ') }),
  ].filter(Boolean);

  return (
    <Stack spacing={0.75} sx={{ mt: 0.75 }} data-testid="tracker-status">
      {trackerWarnings(status, offered).map((warning) => (
        <Alert key={warning} severity="warning" sx={{ py: 0 }}>
          {t(`tracker.status.warnings.${warning}`, {
            version: status.firmware,
            offered,
          })}
        </Alert>
      ))}
      <Typography variant="body2" sx={{ color: 'text.secondary' }}>
        {[
          ...facts,
          statusAt &&
            t('tracker.status.reported', { date: format.dateTime(statusAt) }),
        ]
          .filter(Boolean)
          .join(' · ')}
      </Typography>
    </Stack>
  );
}
