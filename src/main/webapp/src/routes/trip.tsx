import { LinearProgress } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { shortcutCar } from '@/utils/lastCar';

/**
 * Target of the shortcut "Trip" of the installed app: the trips of the last
 * opened car with "Start trip" or "Finish trip", the garage without cars
 */
export const Route = createFileRoute('/trip')({
  component: TripShortcut,
});

function TripShortcut() {
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
        to: '/car/$id/trips',
        params: { id: `${car.id}` },
        replace: true,
      });
    }
  }, [cars, navigate]);

  return <LinearProgress aria-label={t('app.loading')} />;
}
