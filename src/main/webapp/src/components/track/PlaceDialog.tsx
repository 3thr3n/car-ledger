import {
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
} from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Place } from '@/generated';
import {
  deletePlaceNameMutation,
  namePlaceMutation,
  renamePlaceMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';

/** same as PlaceInput.MAX_NAME of the backend */
const MAX_NAME = 100;

export interface PlaceDialogProps {
  carId: number;
  /** the place to name, closed without one */
  place?: Place;
  onClose: () => void;
}

/**
 * Names a place of the map, renames a named one or removes its name
 */
export default function PlaceDialog({
  carId,
  place,
  onClose,
}: PlaceDialogProps) {
  return (
    <Dialog open={!!place} onClose={onClose} maxWidth="xs" fullWidth>
      {/* a new form for each place, with its name */}
      {place && (
        <PlaceForm
          key={`${place.id}-${place.lat}-${place.lon}`}
          carId={carId}
          place={place}
          onClose={onClose}
        />
      )}
    </Dialog>
  );
}

function PlaceForm({
  carId,
  place,
  onClose,
}: PlaceDialogProps & { place: Place }) {
  const { t } = useTranslation();
  const [name, setName] = useState(place.name ?? '');

  const done = (message: string) => async () => {
    toast.success(message);
    onClose();
    await invalidateCar(carId);
  };
  const onError = (error: unknown) =>
    toast.error(errorMessage(error, t('errors.backend')));

  const create = useMutation({
    ...namePlaceMutation({ client: localClient }),
    onSuccess: done(t('track.places.saved')),
    onError,
  });
  const rename = useMutation({
    ...renamePlaceMutation({ client: localClient }),
    onSuccess: done(t('track.places.saved')),
    onError,
  });
  const remove = useMutation({
    ...deletePlaceNameMutation({ client: localClient }),
    onSuccess: done(t('track.places.removed')),
    onError,
  });
  const pending = create.isPending || rename.isPending || remove.isPending;
  const valid = name.trim().length > 0 && name.trim().length <= MAX_NAME;

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    if (!valid) return;
    if (place.id != null) {
      rename.mutate({
        path: { carId, id: place.id },
        body: { name: name.trim() },
      });
    } else {
      create.mutate({
        path: { carId },
        body: { lat: place.lat, lon: place.lon, name: name.trim() },
      });
    }
  };

  return (
    <form onSubmit={submit} noValidate>
      <DialogTitle>
        {place.name
          ? t('track.places.renameTitle', { name: place.name })
          : t('track.places.title')}
      </DialogTitle>
      <DialogContent>
        <TextField
          autoFocus
          fullWidth
          margin="dense"
          label={t('track.places.name')}
          value={name}
          onChange={(event) => setName(event.target.value)}
          helperText={t('track.places.nameHelp')}
          slotProps={{ htmlInput: { maxLength: MAX_NAME } }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        {place.id != null && (
          <Button
            color="error"
            disabled={pending}
            onClick={() => remove.mutate({ path: { carId, id: place.id! } })}
            sx={{ mr: 'auto' }}
          >
            {t('track.places.removeName')}
          </Button>
        )}
        <Button onClick={onClose} color="inherit">
          {t('button.cancel')}
        </Button>
        <Button type="submit" variant="contained" disabled={!valid || pending}>
          {t('button.save')}
        </Button>
      </DialogActions>
    </form>
  );
}
