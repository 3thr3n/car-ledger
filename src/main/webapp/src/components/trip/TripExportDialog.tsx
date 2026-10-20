import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  MenuItem,
  Stack,
  TextField,
} from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import { DatePicker } from '@mui/x-date-pickers';
import { Dayjs } from 'dayjs';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { baseUrl } from '@/utils/QueryClient';

/** a year, all trips or a period of days */
type Period = number | 'all' | 'period';

/**
 * Exports the trip log as PDF (Fahrtenbuch) or CSV, for a year or a period.
 * The days and the times are the ones of this browser, the PDF is in its
 * language.
 */
export default function TripExportDialog({
  carId,
  years,
  year,
  open,
  onClose,
}: {
  carId: number;
  /** the years with trips, the latest first */
  years: number[];
  /** the year chosen in the list, the latest without */
  year?: number;
  open: boolean;
  onClose: () => void;
}) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      {open && (
        <ExportForm
          carId={carId}
          years={years}
          initial={year ?? years[0] ?? 'all'}
          onClose={onClose}
        />
      )}
    </Dialog>
  );
}

function ExportForm({
  carId,
  years,
  initial,
  onClose,
}: {
  carId: number;
  years: number[];
  initial: Period;
  onClose: () => void;
}) {
  const { t, i18n } = useTranslation();
  const [period, setPeriod] = useState<Period>(initial);
  const [from, setFrom] = useState<Dayjs | null>(null);
  const [to, setTo] = useState<Dayjs | null>(null);

  const invalid =
    period === 'period' &&
    (!from?.isValid() || !to?.isValid() || to.isBefore(from, 'day'));
  const query = new URLSearchParams({
    zone: Intl.DateTimeFormat().resolvedOptions().timeZone,
  });
  if (typeof period === 'number') query.set('year', `${period}`);
  if (period === 'period' && !invalid) {
    query.set('from', from!.format('YYYY-MM-DD'));
    query.set('to', to!.format('YYYY-MM-DD'));
  }
  const url = (format: 'csv' | 'pdf') => {
    const params = new URLSearchParams(query);
    if (format === 'pdf')
      params.set('language', i18n.language.startsWith('de') ? 'de' : 'en');
    return `${baseUrl}/api/export/${carId}/trips/${format}?${params}`;
  };

  return (
    <>
      <DialogTitle>{t('trip.export.title')}</DialogTitle>
      <DialogContent>
        <Stack spacing={2.5} sx={{ pt: 1 }}>
          <TextField
            select
            label={t('trip.export.period')}
            value={period}
            onChange={(event) => {
              const value = event.target.value;
              setPeriod(
                value === 'all' || value === 'period' ? value : Number(value),
              );
            }}
            fullWidth
          >
            {years.map((option) => (
              <MenuItem key={option} value={option}>
                {option}
              </MenuItem>
            ))}
            <MenuItem value="all">{t('common.allYears')}</MenuItem>
            <MenuItem value="period">{t('trip.export.custom')}</MenuItem>
          </TextField>
          {period === 'period' && (
            <Stack direction="row" spacing={2}>
              <DatePicker
                label={t('trip.export.from')}
                value={from}
                onChange={setFrom}
                slotProps={{ textField: { fullWidth: true, required: true } }}
              />
              <DatePicker
                label={t('trip.export.to')}
                value={to}
                onChange={setTo}
                minDate={from ?? undefined}
                slotProps={{ textField: { fullWidth: true, required: true } }}
              />
            </Stack>
          )}
          <Alert severity="info" variant="outlined">
            {t('trip.export.text')}
          </Alert>
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          href={url('csv')}
          disabled={invalid}
          startIcon={<FileDownloadRounded />}
        >
          CSV
        </Button>
        <Button
          variant="contained"
          href={url('pdf')}
          disabled={invalid}
          startIcon={<FileDownloadRounded />}
        >
          PDF
        </Button>
      </DialogActions>
    </>
  );
}
