import {
  Box,
  ButtonBase,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ChangeEvent, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  deleteAttachmentMutation,
  getAttachmentLimitsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { fileSize } from '@/utils/format';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import useObjectUrl from '@/hooks/useObjectUrl';
import AttachmentTile, { TILE } from './AttachmentTile';
import {
  ACCEPT,
  attachmentsOptions,
  contentUrl,
  uploadAttachment,
  previewUrl,
} from './attachments';

export interface BillAttachmentsProps {
  carId: number;
  /** existing bill, its files are uploaded at once */
  billId?: number;
  /** whether the bill is a recurring cost, its files are e.g. the contract */
  recurring?: boolean;
  /** files of a new bill, uploaded after it is saved */
  pending: File[];
  onPendingChange: (files: File[]) => void;
}

/**
 * Receipts and other files of a bill: thumbnails that open the file, a button
 * to add photos (the camera on phones) or PDFs
 */
export default function BillAttachments({
  carId,
  billId,
  recurring,
  pending,
  onPendingChange,
}: BillAttachmentsProps) {
  const { t, i18n } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  // set up by the operator, they change only with a new start of the server
  const { data: limits } = useQuery({
    ...getAttachmentLimitsOptions({ client: localClient }),
    staleTime: Infinity,
  });
  const [toDelete, setToDelete] = useState<number>();

  const { data: attachments } = useQuery({
    ...attachmentsOptions(carId, billId ?? 0, recurring),
    enabled: billId !== undefined,
  });

  const upload = useMutation({
    mutationFn: async (files: File[]) => {
      for (const file of files) {
        await uploadAttachment(carId, billId!, file, recurring);
      }
    },
    onSettled: () => invalidateCar(carId),
    onError: (error) =>
      toast.error(errorMessage(error, t('attachments.failed'))),
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
      <Typography variant="subtitle2" component="h2" sx={{ mb: 1 }}>
        {t('attachments.title')}
      </Typography>
      <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
        {attachments?.map((attachment) => (
          <AttachmentTile
            key={attachment.id}
            name={attachment.fileName ?? ''}
            pdf={attachment.contentType === 'application/pdf'}
            url={contentUrl(carId, attachment.id!)}
            previewUrl={previewUrl(carId, attachment.id!)}
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
        {limits
          ? t('attachments.helpLimits', {
              size: fileSize(i18n.language, limits.maxSize),
              count: limits.maxFiles,
            })
          : t('attachments.help')}
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
  const url = useObjectUrl(file);

  return (
    <AttachmentTile name={file.name} pdf={pdf} url={url} onRemove={onRemove} />
  );
}
