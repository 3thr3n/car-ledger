import { LinearProgress } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { shortcutCar, shortcutType } from '@/utils/lastCar';

/**
 * Target of the shortcut "Add refuel" of the installed app: the form of a new
 * refuel (or charge) of the last opened car, the garage without cars
 */
export const Route = createFileRoute('/refuel')({
  component: RefuelShortcut,
});

function RefuelShortcut() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));

  useEffect(() => {
    if (!cars) return;
    const car = shortcutCar(cars);
    if (car?.id === undefined) {
      navigate({ to: '/car', replace: true });
    } else {
      navigate({
        to: '/car/$id/bill/add',
        params: { id: `${car.id}` },
        search: { type: shortcutType(car) },
        replace: true,
      });
    }
  }, [cars, navigate]);

  return <LinearProgress aria-label={t('app.loading')} />;
}
