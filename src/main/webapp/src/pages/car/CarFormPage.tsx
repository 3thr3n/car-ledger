import {
  Box,
  Button,
  Card,
  Grid,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { DatePicker } from '@mui/x-date-pickers';
import dayjs, { Dayjs } from 'dayjs';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate, useRouter } from '@tanstack/react-router';
import { SubmitEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { Car, createCar, CarInput } from '@/generated';
import { toApiDate, useFormat } from '@/utils/format';
import { normalizeVin, vinError } from '@/utils/vin';
import CarPictureField from '@/components/car/CarPictureField';
import { uploadCarPicture } from '@/components/car/carPicture';
import {
  deleteMyCarMutation,
  getMyCarOptions,
  updateMyCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { invalidateCar, localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import NumberField from '@/components/bill/NumberField';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';
import ConfirmDialog from '@/components/ui/ConfirmDialog';

export interface CarFormPageProps {
  /** set when editing an existing car */
  id?: number;
}

const currentYear = new Date().getFullYear();

export default function CarFormPage({ id }: CarFormPageProps) {
  const { t } = useTranslation();
  const { data: car, isLoading } = useQuery({
    ...getMyCarOptions({ client: localClient, path: { id: id ?? 0 } }),
    enabled: id !== undefined,
  });

  if (id !== undefined && (isLoading || !car)) {
    return null;
  }

  return (
    <Page id="CarFormPage" maxWidth="sm">
      <PageHeader
        title={id ? t('car.form.editTitle') : t('car.form.newTitle')}
        subtitle={id ? t('car.form.editSubtitle') : t('car.form.newSubtitle')}
      />
      {/* keyed, so the form state is initialized from the loaded car */}
      <CarForm
        key={car?.id ?? 'new'}
        id={id}
        initial={car ?? {}}
        current={car?.currentOdometer.value}
      />
      {id !== undefined && car && (
        <DeleteCar
          id={id}
          name={car.name ?? ''}
          entries={car.amountBills ?? 0}
        />
      )}
    </Page>
  );
}

/**
 * @param current odometer of an existing car including the refuels since its
 *   last reading, the form starts with it
 */
function CarForm({
  id,
  initial,
  current,
}: {
  id?: number;
  /** the loaded car, its picture follows the saved one */
  initial: CarInput & Pick<Car, 'pictureId'>;
  current?: number;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const router = useRouter();

  const [name, setName] = useState(initial.name ?? '');
  const [year, setYear] = useState<number | undefined>(
    initial.year ?? currentYear,
  );
  const [odometer, setOdometer] = useState<number | undefined>(
    current ?? initial.odometer ?? 0,
  );
  const [licensePlate, setLicensePlate] = useState(initial.licensePlate ?? '');
  const [vin, setVin] = useState(initial.vin ?? '');
  const [purchaseDate, setPurchaseDate] = useState<Dayjs | null>(
    initial.purchaseDate ? dayjs(initial.purchaseDate) : null,
  );
  const [purchasePrice, setPurchasePrice] = useState<number | undefined>(
    initial.purchasePrice,
  );
  const [picture, setPicture] = useState<File>();
  const [touched, setTouched] = useState(false);
  const format = useFormat();

  const errors = {
    name: !name.trim() ? t('car.form.errors.name') : undefined,
    year:
      !year || year < 1900 || year > currentYear + 1
        ? t('car.form.errors.year')
        : undefined,
    odometer:
      odometer === undefined || odometer < 0
        ? t('car.form.errors.odometer')
        : undefined,
    vin: vinError(vin, year) && t(`car.form.errors.${vinError(vin, year)}`),
    purchaseDate:
      purchaseDate &&
      (!purchaseDate.isValid() || purchaseDate.isAfter(dayjs(), 'day'))
        ? t('car.form.errors.purchaseDate')
        : undefined,
  };
  const valid = Object.values(errors).every((error) => !error);

  const onError = (error: Error) => {
    toast.error(errorMessage(error, t('errors.backend')));
  };

  const create = useMutation({
    mutationFn: async (body: CarInput) => {
      const { response } = await createCar({
        client: localClient,
        body,
        throwOnError: true,
      });
      return response;
    },
    onSuccess: async (response) => {
      toast.success(t('car.form.saved'));
      await queryClient.invalidateQueries();
      // the API answers with the location of the new car
      const newId = response.headers.get('Location')?.split('/').pop();
      if (newId && picture) {
        // the car is created even if its picture fails, it can be added later
        try {
          await uploadCarPicture(Number(newId), picture);
          await queryClient.invalidateQueries();
        } catch (error) {
          toast.error(errorMessage(error, t('car.picture.failed')));
        }
      }
      if (newId) {
        await navigate({
          to: '/car/$id',
          params: { id: newId },
          replace: true,
        });
      } else {
        await navigate({ to: '/car' });
      }
    },
    onError,
  });

  const update = useMutation({
    ...updateMyCarMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('car.form.updated'));
      await invalidateCar(id!);
      await navigate({
        to: '/car/$id',
        params: { id: `${id}` },
        replace: true,
      });
    },
    onError,
  });

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    setTouched(true);
    if (!valid) return;

    // an untouched estimate keeps the last reading, a changed one is a new reading
    const body = {
      name: name.trim(),
      year,
      odometer:
        current !== undefined && odometer === current
          ? initial.odometer
          : odometer,
      licensePlate: licensePlate.trim() || undefined,
      vin: normalizeVin(vin) || undefined,
      purchaseDate: toApiDate(purchaseDate),
      purchasePrice,
    };
    if (id) {
      update.mutate({ path: { id }, body });
    } else {
      create.mutate(body);
    }
  };

  return (
    <Card component="form" onSubmit={submit} noValidate sx={{ p: 3 }}>
      <Stack spacing={2.5}>
        <CarPictureField
          carId={id}
          name={name}
          pictureId={initial.pictureId}
          pending={picture}
          onPendingChange={setPicture}
        />
        <TextField
          autoFocus
          label={t('car.form.name')}
          placeholder={t('car.form.namePlaceholder')}
          value={name}
          onChange={(e) => setName(e.target.value)}
          error={touched && !!errors.name}
          helperText={touched && errors.name}
          required
        />
        <NumberField
          label={t('car.form.year')}
          value={year}
          onChange={setYear}
          decimalScale={0}
          thousandSeparator={false}
          error={touched ? errors.year : undefined}
          required
        />
        <NumberField
          label={t('car.form.odometer')}
          value={odometer}
          onChange={setOdometer}
          decimalScale={0}
          suffix=" km"
          helperText={t(
            id === undefined
              ? 'car.form.odometerHelp'
              : 'car.form.odometerHelpEdit',
          )}
          error={touched ? errors.odometer : undefined}
          required
        />
        <Box>
          <Typography
            variant="subtitle1"
            sx={{
              fontWeight: 650,
            }}
          >
            {t('car.form.details')}
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: 'text.secondary',
              mb: 2,
            }}
          >
            {t('car.form.detailsHelp')}
          </Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 5 }}>
              <TextField
                label={t('car.form.licensePlate')}
                placeholder={t('car.form.licensePlatePlaceholder')}
                value={licensePlate}
                onChange={(e) => setLicensePlate(e.target.value)}
                slotProps={{ htmlInput: { maxLength: 20 } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 7 }}>
              <TextField
                label={t('car.form.vin')}
                value={vin}
                onChange={(e) => setVin(e.target.value.toUpperCase())}
                error={touched && !!errors.vin}
                helperText={
                  (touched && errors.vin) ||
                  t('car.form.vinHelp', { count: normalizeVin(vin).length })
                }
                slotProps={{
                  htmlInput: {
                    maxLength: 21,
                    autoCapitalize: 'characters',
                    spellCheck: false,
                  },
                }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <DatePicker
                label={t('car.form.purchaseDate')}
                value={purchaseDate}
                onChange={setPurchaseDate}
                disableFuture
                slotProps={{
                  field: { clearable: true },
                  textField: {
                    error: touched && !!errors.purchaseDate,
                    helperText: touched && errors.purchaseDate,
                  },
                }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <NumberField
                label={t('car.form.purchasePrice')}
                value={purchasePrice}
                onChange={setPurchasePrice}
                suffix={` ${format.currencySymbol}`}
                max={9_999_999_999}
              />
            </Grid>
          </Grid>
        </Box>
        <Stack
          direction="row"
          spacing={1}
          sx={{
            justifyContent: 'flex-end',
          }}
        >
          <Button color="inherit" onClick={() => router.history.back()}>
            {t('button.cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={create.isPending || update.isPending}
          >
            {t('button.save')}
          </Button>
        </Stack>
      </Stack>
    </Card>
  );
}

function DeleteCar({
  id,
  name,
  entries,
}: {
  id: number;
  name: string;
  entries: number;
}) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);

  const mutation = useMutation({
    ...deleteMyCarMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('car.delete.done'));
      // leave the pages of the car first, they would load it again otherwise
      await navigate({ to: '/car', replace: true });
      await invalidateCar(id);
    },
    onError: () => {
      toast.error(t('errors.backend'));
    },
  });

  return (
    <Card sx={{ p: 3, mt: 3, borderColor: 'error.main' }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        spacing={2}
        sx={{
          alignItems: { sm: 'center' },
          justifyContent: 'space-between',
        }}
      >
        <Box>
          <Typography
            sx={{
              fontWeight: 600,
            }}
          >
            {t('car.delete.title')}
          </Typography>
          <Typography
            variant="body2"
            sx={{
              color: 'text.secondary',
            }}
          >
            {t('car.delete.text', { count: entries })}
          </Typography>
        </Box>
        <Button
          color="error"
          variant="outlined"
          disabled={mutation.isPending}
          onClick={() => setOpen(true)}
          sx={{ flexShrink: 0 }}
        >
          {t('car.delete.button')}
        </Button>
      </Stack>
      <ConfirmDialog
        open={open}
        title={t('car.delete.confirmTitle', { name })}
        text={t('car.delete.confirmText', { count: entries })}
        confirmLabel={t('car.delete.button')}
        onClose={() => setOpen(false)}
        onConfirm={() => mutation.mutate({ path: { id } })}
      />
    </Card>
  );
}
