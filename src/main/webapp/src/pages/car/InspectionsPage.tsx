import { Button, Stack, useMediaQuery, useTheme } from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import FactCheckRounded from '@mui/icons-material/FactCheckRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { TechnicalInspection } from '@/generated';
import {
  deleteTechnicalInspectionMutation,
  getMyCarOptions,
  getTechnicalInspectionsOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import CarPapersDialog from '@/components/papers/CarPapersDialog';
import OfflineDocumentsPanel from '@/components/offline/OfflineDocumentsPanel';
import InspectionList from '@/components/inspection/InspectionList';
import InspectionDialog from '@/components/inspection/InspectionDialog';

/** the HUs of the car with their reports, the latest first */
export default function InspectionsPage({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isPhone = useMediaQuery(theme.breakpoints.down('sm'));
  const [formOpen, setFormOpen] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [toEdit, setToEdit] = useState<TechnicalInspection>();
  const [toDelete, setToDelete] = useState<TechnicalInspection>();

  const { data: inspections = [], isLoading } = useQuery(
    getTechnicalInspectionsOptions({ client: localClient, path: { carId } }),
  );
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );

  const remove = useMutation({
    ...deleteTechnicalInspectionMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('inspection.deleted'));
      await invalidateCar(carId);
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const openForm = (inspection?: TechnicalInspection) => {
    setToEdit(inspection);
    setFormOpen(true);
  };

  const addButton = (
    <Button
      variant="contained"
      size="small"
      startIcon={<AddRounded />}
      onClick={() => openForm()}
      fullWidth={isPhone}
      sx={{ flexShrink: 0, whiteSpace: 'nowrap' }}
    >
      {t('inspection.add.button')}
    </Button>
  );

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
      title={t('inspection.title')}
      subtitle={t('inspection.subtitle')}
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
      {isPhone && (
        <Stack spacing={1} sx={{ mb: 1 }}>
          {addButton}
          {papersButton}
        </Stack>
      )}
      {!isLoading && inspections.length === 0 ? (
        <EmptyState
          icon={<FactCheckRounded />}
          title={t('inspection.empty.title')}
          text={t('inspection.empty.text')}
        />
      ) : (
        <InspectionList
          carId={carId}
          inspections={inspections}
          onEdit={openForm}
          onDelete={setToDelete}
        />
      )}

      <Stack sx={{ mt: 2 }}>
        <OfflineDocumentsPanel carId={carId} />
      </Stack>

      <InspectionDialog
        open={formOpen}
        carId={carId}
        odometer={car?.currentOdometer.value}
        inspection={toEdit}
        onClose={() => setFormOpen(false)}
      />
      <CarPapersDialog
        carId={carId}
        open={exporting}
        onClose={() => setExporting(false)}
      />
      <ConfirmDialog
        open={!!toDelete}
        title={t('inspection.delete.title')}
        text={t('inspection.delete.text')}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete && remove.mutate({ path: { carId, id: toDelete.id } })
        }
      />
    </Section>
  );
}
