import {
  Box,
  Button,
  CircularProgress,
  Menu,
  MenuItem,
  Stack,
  Typography,
} from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { ChangeEvent, useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { addModificationDocument, DocumentKind } from '@/generated';
import {
  deleteAttachmentMutation,
  getModificationDocumentsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { prepareUpload } from '@/utils/image';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import AttachmentTile from '@/components/bill/AttachmentTile';
import { ACCEPT, contentUrl, previewUrl } from '@/components/bill/attachments';
import { useOpenKept } from '@/components/offline/useOfflineDocuments';

export interface ModificationDocumentsProps {
  carId: number;
  modificationId: number;
}

/**
 * The Gutachten, the Abnahme and photos of a modification by their kind. The
 * kind is picked first, then the files.
 */
export default function ModificationDocuments({
  carId,
  modificationId,
}: ModificationDocumentsProps) {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);
  const [menu, setMenu] = useState<HTMLElement>();
  const [kind, setKind] = useState<DocumentKind>(DocumentKind.APPROVAL);
  const [toDelete, setToDelete] = useState<number>();

  const { data: documents = [] } = useQuery(
    getModificationDocumentsOptions({
      client: localClient,
      path: { carId, modificationId },
    }),
  );

  const upload = useMutation({
    mutationFn: async ({
      files,
      kind,
    }: {
      files: File[];
      kind: DocumentKind;
    }) => {
      for (const file of files) {
        await addModificationDocument({
          client: localClient,
          path: { carId, modificationId },
          body: { file: await prepareUpload(file), kind },
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

  // without a connection the kept copy, if there is one
  const open = useOpenKept();

  const pick = (chosen: DocumentKind) => {
    setMenu(undefined);
    setKind(chosen);
    input.current?.click();
  };

  const add = (event: ChangeEvent<HTMLInputElement>) => {
    const files = [...(event.target.files ?? [])];
    // the same file can be picked again
    event.target.value = '';
    if (files.length) upload.mutate({ files, kind });
  };

  const kinds = Object.values(DocumentKind).filter((each) =>
    documents.some((document) => document.kind === each),
  );

  return (
    <Box data-testid="modification-documents">
      <Stack
        direction="row"
        sx={{ alignItems: 'center', justifyContent: 'space-between', mb: 1 }}
      >
        <Typography variant="subtitle2">
          {t('modification.documents.title')}
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
          onClick={(event) => setMenu(event.currentTarget)}
        >
          {t('modification.documents.add')}
        </Button>
        <Menu anchorEl={menu} open={!!menu} onClose={() => setMenu(undefined)}>
          {Object.values(DocumentKind).map((each) => (
            <MenuItem key={each} onClick={() => pick(each)}>
              {t(`modification.documents.kinds.${each}`)}
            </MenuItem>
          ))}
        </Menu>
      </Stack>
      {documents.length === 0 ? (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {t('modification.documents.empty')}
        </Typography>
      ) : (
        <Stack spacing={1.5}>
          {kinds.map((each) => (
            <Box key={each}>
              <Typography
                variant="caption"
                sx={{ display: 'block', color: 'text.secondary', mb: 0.5 }}
              >
                {t(`modification.documents.kinds.${each}`)}
              </Typography>
              <Stack direction="row" sx={{ flexWrap: 'wrap', gap: 1 }}>
                {documents
                  .filter((document) => document.kind === each)
                  .map((document) => (
                    <AttachmentTile
                      key={document.id}
                      name={document.fileName ?? ''}
                      pdf={document.contentType === 'application/pdf'}
                      url={contentUrl(carId, document.id!)}
                      previewUrl={previewUrl(carId, document.id!)}
                      onRemove={() => setToDelete(document.id)}
                      onOpen={open(contentUrl(carId, document.id!))}
                    />
                  ))}
              </Stack>
            </Box>
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
        data-testid="document-input"
      />
      <ConfirmDialog
        open={toDelete !== undefined}
        title={t('attachments.deleteTitle')}
        text={t('modification.documents.deleteText')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete !== undefined &&
          remove.mutate({ path: { carId, id: toDelete } })
        }
      />
    </Box>
  );
}
