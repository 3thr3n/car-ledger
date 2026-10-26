import { useQuery } from '@tanstack/react-query';
import {
  getMapSettingsOptions,
  getMyCarOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { can } from '@/utils/carRole';

/**
 * The style of the map, when the map is set up, the car has a GPS tracker and
 * the user sees its tracks (not a driver): only then the tracks are shown
 */
export default function useTrackMap(carId: number): string | undefined {
  const { data: settings } = useQuery(
    getMapSettingsOptions({ client: localClient }),
  );
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  return settings?.styleUrl && car?.tracked && can(car, 'READ')
    ? settings.styleUrl
    : undefined;
}
