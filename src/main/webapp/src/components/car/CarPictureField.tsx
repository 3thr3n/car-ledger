import { Avatar, Box, Button, Stack, Typography } from '@mui/material';
import PhotoCameraRounded from '@mui/icons-material/PhotoCameraRounded';
import { useMutation } from '@tanstack/react-query';
import { ChangeEvent, useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { deleteCarPictureMutation } from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import CarAvatar from '@/components/layout/CarAvatar';
import useObjectUrl from '@/hooks/useObjectUrl';
import { uploadCarPicture } from './carPicture';

const SIZE = 72;

/**
 * Picture of the car in its form. An existing car saves a new picture at once,
 * a new one keeps it as pending until the car is created.
 */
export default function CarPictureField({
  carId,
  name,
  pictureId,
  pending,
  onPendingChange,
}: {
  carId?: number;
  name: string;
  pictureId?: number;
  pending?: File;
  onPendingChange: (file: File | undefined) => void;
}) {
  const { t } = useTranslation();
  const input = useRef<HTMLInputElement>(null);

  const upload = useMutation({
    mutationFn: (file: File) => uploadCarPicture(carId!, file),
    onSuccess: () => toast.success(t('car.picture.saved')),
    onError: (error) =>
      toast.error(errorMessage(error, t('car.picture.failed'))),
    onSettled: () => invalidateCar(carId!),
  });
  const remove = useMutation({
    ...deleteCarPictureMutation({ client: localClient }),
    onSuccess: () => {
      toast.success(t('car.picture.removed'));
      return invalidateCar(carId!);
    },
    onError: () => toast.error(t('errors.backend')),
  });

  const choose = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // the same file can be picked again
    event.target.value = '';
    if (!file) return;
    if (carId === undefined) {
      onPendingChange(file);
    } else {
      upload.mutate(file);
    }
  };

  // the API sends null without a picture
  const hasPicture = pending !== undefined || pictureId != null;
  const busy = upload.isPending || remove.isPending;

  return (
    <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
      {pending ? (
        <PendingPicture file={pending} name={name} />
      ) : (
        <CarAvatar id={carId} name={name} pictureId={pictureId} size={SIZE} />
      )}
      <Box>
        <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
          <Button
            variant="outlined"
            size="small"
            startIcon={<PhotoCameraRounded />}
            disabled={busy}
            onClick={() => input.current?.click()}
          >
            {hasPicture ? t('car.picture.change') : t('car.picture.choose')}
          </Button>
          {hasPicture && (
            <Button
              color="inherit"
              size="small"
              disabled={busy}
              onClick={() =>
                carId === undefined
                  ? onPendingChange(undefined)
                  : remove.mutate({ path: { id: carId } })
              }
            >
              {t('car.picture.remove')}
            </Button>
          )}
        </Stack>
        <Typography
          variant="caption"
          sx={{ display: 'block', mt: 0.75, color: 'text.secondary' }}
        >
          {t('car.picture.help')}
        </Typography>
      </Box>
      <input
        ref={input}
        type="file"
        accept="image/*"
        hidden
        onChange={choose}
        data-testid="car-picture-input"
      />
    </Stack>
  );
}

/** preview of the picked photo, cropped to the square like the saved one */
function PendingPicture({ file, name }: { file: File; name: string }) {
  const url = useObjectUrl(file);
  return (
    <Avatar
      variant="rounded"
      src={url}
      alt={name}
      sx={{ width: SIZE, height: SIZE, borderRadius: SIZE / 4 }}
    />
  );
}
