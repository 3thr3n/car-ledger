import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Radio,
  RadioGroup,
} from '@mui/material';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { baseUrl } from '@/utils/QueryClient';

type Content = 'current' | 'history';

/**
 * The modifications and the HUs of the car as one PDF with their documents and
 * reports, in the language of this browser
 */
export default function CarPapersDialog({
  carId,
  open,
  onClose,
}: {
  carId: number;
  open: boolean;
  onClose: () => void;
}) {
  const { t, i18n } = useTranslation();
  const [content, setContent] = useState<Content>('current');

  const params = new URLSearchParams({
    language: i18n.language.startsWith('de') ? 'de' : 'en',
  });
  if (content === 'history') params.set('history', 'true');
  const url = `${baseUrl}/api/export/${carId}/papers/pdf?${params}`;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{t('papers.title')}</DialogTitle>
      <DialogContent>
        <RadioGroup
          value={content}
          onChange={(event) => setContent(event.target.value as Content)}
          sx={{ mb: 2 }}
        >
          {(['current', 'history'] as const).map((each) => (
            <FormControlLabel
              key={each}
              value={each}
              control={<Radio />}
              label={t(`papers.content.${each}`)}
              sx={{
                alignItems: 'flex-start',
                mb: 1,
                '& .MuiRadio-root': { mt: -0.75 },
              }}
            />
          ))}
        </RadioGroup>
        <Alert severity="info" variant="outlined">
          {t('papers.text')}
        </Alert>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button
          variant="contained"
          href={url}
          startIcon={<FileDownloadRounded />}
          onClick={onClose}
        >
          PDF
        </Button>
      </DialogActions>
    </Dialog>
  );
}
