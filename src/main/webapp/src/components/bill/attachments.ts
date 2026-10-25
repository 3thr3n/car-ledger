import { addBillAttachment } from '@/generated';
import { baseUrl, localClient } from '@/utils/QueryClient';
import { prepareUpload } from '@/utils/image';

/** what the file input offers, the camera included on phones */
export const ACCEPT = 'image/*,application/pdf';

/** address of the file, shown inline by the browser */
export const contentUrl = (carId: number, id: number) =>
  `${baseUrl}/api/attachment/${carId}/${id}/content`;

/** a small JPEG of a photo for the tiles, the file itself if it has none */
export const previewUrl = (carId: number, id: number) =>
  `${baseUrl}/api/attachment/${carId}/${id}/preview`;

/**
 * Shrinks photos and attaches the file to the bill
 *
 * @throws BackendError with the reason, e.g. a wrong type
 */
export async function uploadAttachment(
  carId: number,
  billId: number,
  file: File,
) {
  const { data } = await addBillAttachment({
    client: localClient,
    path: { carId, billId },
    body: { file: await prepareUpload(file) },
    throwOnError: true,
  });
  return data;
}
