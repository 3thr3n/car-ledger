import { Button, Card, Stack, TextField } from '@mui/material';
import { useMutation, useQuery } from '@tanstack/react-query';
import { useNavigate, useRouter } from '@tanstack/react-router';
import { FormEvent, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { createCar, CarInput } from '@/generated';
import {
  getMyCarOptions,
  updateMyCarMutation,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { invalidateCar, localClient } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import NumberField from '@/components/bill/NumberField';
import Page from '@/components/layout/Page';
import PageHeader from '@/components/ui/PageHeader';

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
      <CarForm key={car?.id ?? 'new'} id={id} initial={car ?? {}} />
    </Page>
  );
}

function CarForm({ id, initial }: { id?: number; initial: CarInput }) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const router = useRouter();

  const [name, setName] = useState(initial.name ?? '');
  const [year, setYear] = useState<number | undefined>(
    initial.year ?? currentYear,
  );
  const [odometer, setOdometer] = useState<number | undefined>(
    initial.odometer ?? 0,
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

    const body = { name: name.trim(), year, odometer };
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
          helperText={t('car.form.odometerHelp')}
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
