import { QueryKey, UseQueryOptions } from '@tanstack/react-query';
import {
  addBillAttachment,
  addRecurringBillFile,
  Attachment,
} from '@/generated';
import {
  getBillAttachmentsOptions,
  getRecurringBillFilesOptions,
} from '@/generated/@tanstack/react-query.gen';
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
 * @param recurring whether the bill is a recurring cost (e.g. its contract)
 * @throws BackendError with the reason, e.g. a wrong type
 */
export async function uploadAttachment(
  carId: number,
  billId: number,
  file: File,
  recurring = false,
) {
  const body = { file: await prepareUpload(file) };
  const { data } = recurring
    ? await addRecurringBillFile({
        client: localClient,
        path: { carId, recurringBillId: billId },
        body,
        throwOnError: true,
      })
    : await addBillAttachment({
        client: localClient,
        path: { carId, billId },
        body,
        throwOnError: true,
      });
  return data;
}

/**
 * Query of the files of a bill, with the generated key so that
 * `invalidateCar` finds it
 *
 * @param recurring whether the bill is a recurring cost
 */
export const attachmentsOptions = (
  carId: number,
  billId: number,
  recurring = false,
) =>
  (recurring
    ? getRecurringBillFilesOptions({
        client: localClient,
        path: { carId, recurringBillId: billId },
      })
    : getBillAttachmentsOptions({
        client: localClient,
        path: { carId, billId },
      })) as UseQueryOptions<Attachment[], Error, Attachment[], QueryKey>;
