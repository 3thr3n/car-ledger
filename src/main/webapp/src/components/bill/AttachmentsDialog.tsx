import {
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  Typography,
} from '@mui/material';
import OpenInNewRounded from '@mui/icons-material/OpenInNewRounded';
import PictureAsPdfRounded from '@mui/icons-material/PictureAsPdfRounded';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { attachmentsOptions, contentUrl } from './attachments';

export interface AttachmentsDialogProps {
  carId: number;
  /** the bill whose files are shown, closed without one */
  billId?: number;
  /** whether the bill is a recurring cost */
  recurring?: boolean;
  onClose: () => void;
  /** opens the edit form of the bill, e.g. to add or remove files */
  onEdit?: () => void;
}

/**
 * The receipts and files of a bill without opening its form: photos are shown
 * at once, each file opens in a new tab
 */
export default function AttachmentsDialog({
  carId,
  billId,
  recurring,
  onClose,
  onEdit,
}: AttachmentsDialogProps) {
  const { t } = useTranslation();

  const { data: attachments, isLoading } = useQuery({
    ...attachmentsOptions(carId, billId ?? 0, recurring),
    enabled: billId !== undefined,
  });

  return (
    <Dialog
      open={billId !== undefined}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle>{t('attachments.title')}</DialogTitle>
      <DialogContent>
        {isLoading && (
          <Stack sx={{ alignItems: 'center', py: 4 }}>
            <CircularProgress size={28} />
          </Stack>
        )}
        <Stack spacing={1.5}>
          {attachments?.map((attachment) => {
            const name = attachment.fileName ?? '';
            const url = contentUrl(carId, attachment.id!);
            const pdf = attachment.contentType === 'application/pdf';
            return (
              <Box
                key={attachment.id}
                component="a"
                href={url}
                target="_blank"
                rel="noopener"
                aria-label={t('attachments.open', { name })}
                sx={{
                  display: 'block',
                  borderRadius: 2,
                  overflow: 'hidden',
                  border: 1,
                  borderColor: 'divider',
                  bgcolor: 'action.hover',
                  color: 'text.primary',
                  textDecoration: 'none',
                  '&:hover': { borderColor: 'primary.main' },
                }}
              >
                {!pdf && (
                  <Box
                    component="img"
                    src={url}
                    alt={name}
                    loading="lazy"
                    sx={{
                      display: 'block',
                      width: '100%',
                      maxHeight: '60vh',
                      objectFit: 'contain',
                    }}
                  />
                )}
                <Stack
                  direction="row"
                  spacing={1}
                  sx={{ alignItems: 'center', px: 1.5, py: 1 }}
                >
                  {pdf && <PictureAsPdfRounded color="error" />}
                  <Typography variant="body2" noWrap sx={{ flexGrow: 1 }}>
                    {name}
                  </Typography>
                  <OpenInNewRounded
                    sx={{ fontSize: 18, color: 'text.secondary' }}
                  />
                </Stack>
              </Box>
            );
          })}
        </Stack>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        {onEdit && (
          <Button
            color="inherit"
            onClick={() => {
              onClose();
              onEdit();
            }}
          >
            {t('button.edit')}
          </Button>
        )}
        <Button variant="contained" onClick={onClose}>
          {t('button.close')}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
