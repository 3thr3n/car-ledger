import { useQuery } from '@tanstack/react-query';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { can, CarAction } from '@/utils/carRole';

/**
 * @return whether the user may do the action with the car, from the car the
 *   layout loaded already
 */
export default function useCarRole(carId: number) {
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );
  return (action: CarAction) => can(car, action);
}
