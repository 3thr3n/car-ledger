import {
  Alert,
  Button,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import BuildRounded from '@mui/icons-material/BuildRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import useCarRole from '@/hooks/useCarRole';
import { Modification, ModificationState } from '@/generated';
import {
  deleteModificationMutation,
  getModificationsOptions,
  getMyCarOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import CarPapersDialog from '@/components/papers/CarPapersDialog';
import ModificationList from '@/components/modification/ModificationList';
import ModificationDialog from '@/components/modification/ModificationDialog';
import RemoveModificationDialog from '@/components/modification/RemoveModificationDialog';
import OfflineDocumentsPanel from '@/components/offline/OfflineDocumentsPanel';

type Shown =
  typeof ModificationState.INSTALLED | typeof ModificationState.REMOVED;

/**
 * The parts on the car with their approvals, the removed ones as its history
 */
export default function ModificationsPage({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isPhone = useMediaQuery(theme.breakpoints.down('sm'));
  const [shown, setShown] = useState<Shown>(ModificationState.INSTALLED);
  const [formOpen, setFormOpen] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [toEdit, setToEdit] = useState<Modification>();
  const [toRemove, setToRemove] = useState<Modification>();
  const [toDelete, setToDelete] = useState<Modification>();

  const { data: modifications = [], isLoading } = useQuery(
    getModificationsOptions({
      client: localClient,
      path: { carId },
      query: { state: shown },
    }),
  );
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  const odometer = car?.currentOdometer.value;

  const remove = useMutation({
    ...deleteModificationMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('modification.deleted'));
      await invalidateCar(carId);
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const openForm = (modification?: Modification) => {
    setToEdit(modification);
    setFormOpen(true);
  };

  // the others only see them
  const canChange = useCarRole(carId)('WRITE');
  const addButton = canChange && (
    <Button
      variant="contained"
      size="small"
      startIcon={<AddRounded />}
      onClick={() => openForm()}
      fullWidth={isPhone}
    >
      {t('modification.add.button')}
    </Button>
  );

  const installed = shown === ModificationState.INSTALLED;

  const papersButton = (
    <Button
      size="small"
      startIcon={<FileDownloadRounded />}
      onClick={() => setExporting(true)}
      sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}
    >
      {t('papers.button')}
    </Button>
  );

  return (
    <Section
      title={t('modification.title')}
      subtitle={t('modification.subtitle')}
      // next to the title it would be cut off on a phone
      action={
        !isPhone && (
          <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
            {papersButton}
            {addButton}
          </Stack>
        )
      }
    >
      <Stack spacing={2}>
        {isPhone && addButton}
        {isPhone && papersButton}
        <ToggleButtonGroup
          size="small"
          exclusive
          value={shown}
          onChange={(_, value: Shown | null) => value && setShown(value)}
          aria-label={t('modification.shown.label')}
          sx={{ alignSelf: 'flex-start' }}
        >
          <ToggleButton value={ModificationState.INSTALLED} sx={{ px: 1.5 }}>
            {t('modification.shown.INSTALLED')}
          </ToggleButton>
          <ToggleButton value={ModificationState.REMOVED} sx={{ px: 1.5 }}>
            {t('modification.shown.REMOVED')}
          </ToggleButton>
        </ToggleButtonGroup>
        {!isLoading && modifications.length === 0 ? (
          <EmptyState
            icon={<BuildRounded />}
            title={t(
              installed
                ? 'modification.empty.title'
                : 'modification.empty.removedTitle',
            )}
            text={t(
              installed
                ? 'modification.empty.text'
                : 'modification.empty.removedText',
            )}
          />
        ) : (
          <ModificationList
            readOnly={!canChange}
            carId={carId}
            modifications={modifications}
            onEdit={openForm}
            onRemove={setToRemove}
            onDelete={setToDelete}
          />
        )}
        <OfflineDocumentsPanel carId={carId} />
        <Alert severity="info" variant="outlined">
          {t('modification.originalsHint')}
        </Alert>
      </Stack>

      <ModificationDialog
        open={formOpen}
        carId={carId}
        odometer={odometer}
        modification={toEdit}
        onClose={() => setFormOpen(false)}
      />
      <RemoveModificationDialog
        modification={toRemove}
        odometer={odometer}
        onClose={() => setToRemove(undefined)}
      />
      <CarPapersDialog
        carId={carId}
        open={exporting}
        onClose={() => setExporting(false)}
      />
      <ConfirmDialog
        open={!!toDelete}
        title={t('modification.delete.title', { part: toDelete?.part })}
        text={t(
          toDelete?.installed
            ? 'modification.delete.textInstalled'
            : 'modification.delete.text',
        )}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete && remove.mutate({ path: { carId, id: toDelete.id } })
        }
      />
    </Section>
  );
}
