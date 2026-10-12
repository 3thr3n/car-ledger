import { Box, Button, Card, Stack, TextField, Typography } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate, useRouter } from '@tanstack/react-router';
import { FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { createCar, CarInput } from '@/generated';
import {
  deleteMyCarMutation,
  getMyCarOptions,
  updateMyCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { invalidateCar, localClient } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
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
  initial: CarInput;
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
  const [touched, setTouched] = useState(false);

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
  };
  const valid = !errors.name && !errors.year && !errors.odometer;

  const onError = (error: BackendError) => {
    toast.error(error.message || t('errors.backend'));
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

  const submit = (event: FormEvent) => {
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
        <Stack direction="row" spacing={1} justifyContent="flex-end">
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
        alignItems={{ sm: 'center' }}
        justifyContent="space-between"
      >
        <Box>
          <Typography fontWeight={600}>{t('car.delete.title')}</Typography>
          <Typography variant="body2" color="text.secondary">
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
