import {
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ChangeEvent, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { addTechnicalInspectionFile } from '@/generated';
import {
  deleteAttachmentMutation,
  getTechnicalInspectionFilesOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { prepareUpload } from '@/utils/image';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import AttachmentTile from '@/components/bill/AttachmentTile';
import { ACCEPT, contentUrl } from '@/components/bill/attachments';
import { useOpenKept } from '@/components/offline/useOfflineDocuments';

/** the report of an HU and other files of it */
export default function InspectionFiles({
  carId,
  inspectionId,
}: {
  carId: number;
  inspectionId: number;
}) {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  const [toDelete, setToDelete] = useState<number>();
  // without a connection the kept copy, if there is one
  const open = useOpenKept();

  const { data: files = [] } = useQuery(
    getTechnicalInspectionFilesOptions({
      client: localClient,
      path: { carId, inspectionId },
    }),
  );

  const upload = useMutation({
    mutationFn: async (picked: File[]) => {
      for (const file of picked) {
        await addTechnicalInspectionFile({
          client: localClient,
          path: { carId, inspectionId },
          body: { file: await prepareUpload(file) },
          throwOnError: true,
        });
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
    const picked = [...(event.target.files ?? [])];
    // the same file can be picked again
    event.target.value = '';
    if (picked.length) upload.mutate(picked);
  };

  return (
    <Box data-testid="inspection-files">
      <Stack
        direction="row"
        sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}
      >
        <Typography variant="subtitle2">
          {t('inspection.files.title')}
        </Typography>
        <Button
          size="small"
          startIcon={
            upload.isPending ? (
              <CircularProgress size={16} />
            ) : (
              <AttachFileRounded />
            )
          }
          disabled={upload.isPending}
          onClick={() => input.current?.click()}
        >
          {t('inspection.files.add')}
        </Button>
      </Stack>
      {files.length === 0 ? (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('inspection.files.empty')}
        </Typography>
      ) : (
        <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
          {files.map((file) => (
            <AttachmentTile
              key={file.id}
              name={file.fileName ?? ''}
              pdf={file.contentType === 'application/pdf'}
              url={contentUrl(carId, file.id!)}
              onRemove={() => setToDelete(file.id)}
              onOpen={open(contentUrl(carId, file.id!))}
            />
          ))}
        </Stack>
      )}
      <input
        ref={input}
        type="file"
        accept={ACCEPT}
        multiple
        hidden
        onChange={add}
        data-testid="inspection-file-input"
      />
      <ConfirmDialog
        open={toDelete !== undefined}
        title={t('attachments.deleteTitle')}
        text={t('inspection.files.deleteText')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete !== undefined &&
          remove.mutate({ path: { carId, id: toDelete } })
        }
      />
    </Box>
  );
}
