import {
  Button,
  Stack,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import AddRounded from '@mui/icons-material/AddRounded';
import NotificationsActiveRounded from '@mui/icons-material/NotificationsActiveRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Reminder } from '@/generated';
import {
  deleteReminderMutation,
  getMyCarOptions,
  getRemindersOptions,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import ConfirmDialog from '@/components/ui/ConfirmDialog';
import ReminderList from '@/components/reminder/ReminderList';
import ReminderDialog from '@/components/reminder/ReminderDialog';
import ReminderDoneDialog from '@/components/reminder/ReminderDoneDialog';

export default function RemindersPage({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isPhone = useMediaQuery(theme.breakpoints.down('sm'));
  const [formOpen, setFormOpen] = useState(false);
  const [toEdit, setToEdit] = useState<Reminder>();
  const [toComplete, setToComplete] = useState<Reminder>();
  const [toDelete, setToDelete] = useState<Reminder>();

  const { data: reminders = [], isLoading } = useQuery(
    getRemindersOptions({ client: localClient, path: { carId } }),
  );
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  const odometer = car?.currentOdometer.value;

  const remove = useMutation({
    ...deleteReminderMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('reminder.deleted'));
      await invalidateCar(carId);
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const openForm = (reminder?: Reminder) => {
    setToEdit(reminder);
    setFormOpen(true);
  };

  const addButton = (
    <Button
      variant="contained"
      size="small"
      startIcon={<AddRounded />}
      onClick={() => openForm()}
      fullWidth={isPhone}
    >
      {t('reminder.add.button')}
    </Button>
  );

  return (
    <Section
      title={t('reminder.title')}
      subtitle={t('reminder.subtitle')}
      // next to the title it would be cut off on a phone
      action={!isPhone && addButton}
    >
      {isPhone && <Stack sx={{ mb: 1 }}>{addButton}</Stack>}
      {!isLoading && reminders.length === 0 ? (
        <EmptyState
          icon={<NotificationsActiveRounded />}
          title={t('reminder.empty.title')}
          text={t('reminder.empty.text')}
        />
      ) : (
        <ReminderList
          reminders={reminders}
          onDone={setToComplete}
          onEdit={openForm}
          onDelete={setToDelete}
        />
      )}
      {odometer !== undefined && reminders.length > 0 && (
        <Stack sx={{ mt: 1 }}>
          <Typography
            variant="caption"
            sx={{
              color: 'text.secondary',
            }}
          >
            {t('reminder.odometerInfo')}
          </Typography>
        </Stack>
      )}

      <ReminderDialog
        open={formOpen}
        carId={carId}
        odometer={odometer}
        reminder={toEdit}
        onClose={() => setFormOpen(false)}
      />
      <ReminderDoneDialog
        reminder={toComplete}
        onClose={() => setToComplete(undefined)}
      />
      <ConfirmDialog
        open={!!toDelete}
        title={t('reminder.delete.title', { title: toDelete?.title })}
        onClose={() => setToDelete(undefined)}
        onConfirm={() =>
          toDelete && remove.mutate({ path: { carId, id: toDelete.id } })
        }
      />
    </Section>
  );
}
