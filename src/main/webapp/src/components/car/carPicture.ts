import { setCarPicture } from '@/generated';
import { localClient } from '@/utils/QueryClient';
import { squarePicture } from '@/utils/image';

/**
 * Crops the photo to a square and saves it as picture of the car
 *
 * @throws BackendError with the reason, e.g. not an image
 */
export async function uploadCarPicture(carId: number, file: File) {
  await setCarPicture({
    client: localClient,
    path: { id: carId },
    body: { file: await squarePicture(file) },
    throwOnError: true,
  });
}
