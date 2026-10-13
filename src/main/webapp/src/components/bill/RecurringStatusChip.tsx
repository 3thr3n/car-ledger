import { Chip } from '@mui/material';
import { useTranslation } from 'react-i18next';

/** whether a recurring cost is still running */
export default function RecurringStatusChip({
  finished,
}: {
  finished?: boolean;
}) {
  const { t } = useTranslation();
  return (
    <Chip
      size="small"
      variant="outlined"
      color={finished ? 'default' : 'success'}
      label={finished ? t('bill.status.finished') : t('bill.status.running')}
    />
  );
}
