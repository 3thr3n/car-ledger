import {
  Box,
  ButtonBase,
  CircularProgress,
  IconButton,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import CloseRounded from '@mui/icons-material/CloseRounded';
import PictureAsPdfRounded from '@mui/icons-material/PictureAsPdfRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ChangeEvent, useEffect, useMemo, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  deleteAttachmentMutation,
  getBillAttachmentsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import { ACCEPT, contentUrl, uploadAttachment } from './attachments';

export interface BillAttachmentsProps {
  carId: number;
  /** existing bill, its files are uploaded at once */
  billId?: number;
  /** files of a new bill, uploaded after it is saved */
  pending: File[];
  onPendingChange: (files: File[]) => void;
}

const TILE = 88;

/**
 * Receipts and other files of a bill: thumbnails that open the file, a button
 * to add photos (the camera on phones) or PDFs
 */
export default function BillAttachments({
  carId,
  billId,
  pending,
  onPendingChange,
}: BillAttachmentsProps) {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  const [toDelete, setToDelete] = useState<number>();

  const { data: attachments } = useQuery({
    ...getBillAttachmentsOptions({
      client: localClient,
      path: { carId, billId: billId ?? 0 },
    }),
    enabled: billId !== undefined,
  });

  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      for (const file of files) {
        await uploadAttachment(carId, billId!, file);
      }
    },
    onSettled: () => invalidateCar(carId),
    onError: (error) =>
      toast.error(
        error instanceof BackendError && error.message
          ? error.message
          : t('attachments.failed'),
      ),
  });

  const remove = useMutation({
    ...deleteAttachmentMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('attachments.deleted'));
      return invalidateCar(carId);
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const add = (event: ChangeEvent<HTMLInputElement>) => {
    const files = [...(event.target.files ?? [])];
    // the same file can be picked again
    event.target.value = '';
    if (!files.length) return;
    if (billId === undefined) {
      onPendingChange([...pending, ...files]);
    } else {
      upload.mutate(files);
    }
  };

  return (
    <Box>
      <Typography variant="subtitle2" sx={{ mb: 1 }}>
        {t('attachments.title')}
      </Typography>
      <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
        {attachments?.map((attachment) => (
          <Tile
            key={attachment.id}
            name={attachment.fileName ?? ''}
            pdf={attachment.contentType === 'application/pdf'}
            url={contentUrl(carId, attachment.id!)}
            onRemove={() => setToDelete(attachment.id)}
          />
        ))}
        {pending.map((file, index) => (
          <PendingTile
            key={`${file.name}-${index}`}
            file={file}
            onRemove={() =>
              onPendingChange(pending.filter((_, i) => i !== index))
            }
          />
        ))}
        <ButtonBase
          onClick={() => input.current?.click()}
          disabled={upload.isPending}
          aria-label={t('attachments.add')}
          sx={{
            width: TILE,
            height: TILE,
            borderRadius: 2,
            border: 1,
            borderStyle: 'dashed',
            borderColor: 'divider',
            color: 'text.secondary',
            flexDirection: 'column',
            gap: 0.5,
            '&:hover': { borderColor: 'primary.main', color: 'primary.main' },
          }}
        >
          {upload.isPending ? (
            <CircularProgress size={22} />
          ) : (
            <>
              <AttachFileRounded />
              <Typography variant="caption">{t('attachments.add')}</Typography>
            </>
          )}
        </ButtonBase>
      </Stack>
      <Typography
        variant="caption"
        sx={{ display: 'block', mt: 1, color: 'text.secondary' }}
      >
        {t('attachments.help')}
      </Typography>
      <input
        ref={input}
        type="file"
        accept={ACCEPT}
        multiple
        hidden
        onChange={add}
        data-testid="attachment-input"
      />
      <ConfirmDialog
        open={toDelete !== undefined}
        title={t('attachments.deleteTitle')}
        text={t('attachments.deleteText')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete !== undefined &&
          remove.mutate({ path: { carId, id: toDelete } })
        }
      />
    </Box>
  );
}

function PendingTile({ file, onRemove }: { file: File; onRemove: () => void }) {
  const pdf = file.type === 'application/pdf';
  const url = useMemo(() => URL.createObjectURL(file), [file]);
  useEffect(() => () => URL.revokeObjectURL(url), [url]);

  return <Tile name={file.name} pdf={pdf} url={url} onRemove={onRemove} />;
}

function Tile({
  name,
  pdf,
  url,
  onRemove,
}: {
  name: string;
  pdf: boolean;
  url: string;
  onRemove: () => void;
}) {
  const { t } = useTranslation();

  return (
    <Box sx={{ position: 'relative', width: TILE, height: TILE }}>
      <Tooltip title={name}>
        <Box
          component="a"
          href={url}
          target="_blank"
          rel="noopener"
          aria-label={t('attachments.open', { name })}
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            width: '100%',
            height: '100%',
            borderRadius: 2,
            overflow: 'hidden',
            border: 1,
            borderColor: 'divider',
            bgcolor: 'action.hover',
            color: 'text.secondary',
            textDecoration: 'none',
          }}
        >
          {pdf ? (
            <>
              <PictureAsPdfRounded color="error" />
              <Typography
                variant="caption"
                noWrap
                sx={{ maxWidth: '100%', px: 0.75 }}
              >
                {name}
              </Typography>
            </>
          ) : (
            <Box
              component="img"
              src={url}
              alt={name}
              loading="lazy"
              sx={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          )}
        </Box>
      </Tooltip>
      <IconButton
        size="small"
        onClick={onRemove}
        aria-label={t('attachments.remove', { name })}
        sx={{
          position: 'absolute',
          top: 2,
          right: 2,
          p: 0.25,
          bgcolor: 'background.paper',
          boxShadow: 1,
          '&:hover': { bgcolor: 'background.paper' },
        }}
      >
        <CloseRounded sx={{ fontSize: 16 }} />
      </IconButton>
    </Box>
  );
}
