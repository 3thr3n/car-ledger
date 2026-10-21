import { useQuery } from '@tanstack/react-query';
import {
  getMapSettingsOptions,
  getTrackersOptions,
} from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

/**
 * The style of the map, when the map is set up and the car has a GPS tracker:
 * only then the tracks are shown
 */
export default function useTrackMap(carId: number): string | undefined {
  const { data: settings } = useQuery(
    getMapSettingsOptions({ client: localClient }),
  );
  const { data: trackers } = useQuery({
    ...getTrackersOptions({ client: localClient, path: { carId } }),
    enabled: !!settings?.styleUrl,
  });
  return settings?.styleUrl && trackers?.length ? settings.styleUrl : undefined;
}
