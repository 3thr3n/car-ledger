import { useNavigate } from '@tanstack/react-router';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { BillType } from '@/generated';
import { useQueueRequest } from '@/offline/useQueued';
import { invalidateCar } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { billTypes } from '@/utils/billTypes';
import { uploadAttachment } from './attachments';
import { BillAttachmentsProps } from './BillAttachments';

/**
 * Shared success/error handling of the "add bill" mutations.
 * `again` keeps the user on the form for the next receipt.
 * The files picked for a new bill are attached once it is saved.
 * Without a connection a new bill goes into the offline queue (`queue`).
 *
 * @param billId of the edited bill, its files are uploaded at once
 */
export default function useSaveBill(
  carId: number,
  type: BillType,
  reset: () => void,
  billId?: number,
) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const again = useRef(false);
  const [pending, setPending] = useState<File[]>([]);
  const addToQueue = useQueueRequest();

  /** the bill is saved even if a file fails, the user can attach it later */
  const uploadPending = async (savedId?: number) => {
    if (savedId === undefined) return;
    for (const file of pending) {
      try {
        await uploadAttachment(carId, savedId, file);
      } catch (error) {
        toast.error(
          t('attachments.notAttached', {
            name: file.name,
            reason: errorMessage(error, t('errors.backend')),
          }),
        );
      }
    }
    setPending([]);
  };

  const attachments: BillAttachmentsProps = {
    carId,
    billId,
    pending,
    onPendingChange: setPending,
  };

  const done = async () => {
    if (again.current) {
      reset();
    } else {
      await navigate({
        to: billTypes[type].listRoute,
        params: { id: `${carId}` },
      });
    }
  };

  return {
    attachments,
    markAgain: (value: boolean) => {
      again.current = value;
    },
    /**
     * Keeps a new bill on the device, sent when there is a connection again.
     * The files are not kept, they are attached later.
     *
     * @param id of the bill in the queue, a corrected one keeps its id
     */
    queue: async (id: string, url: string, body: object) => {
      await addToQueue({
        id,
        carId,
        billType: type,
        url,
        body: body as Record<string, unknown>,
      });
      if (pending.length) toast.warning(t('offline.noFiles'));
      setPending([]);
      toast.info(t('offline.queued'));
      await done();
    },
    // the create endpoints answer with the saved bill, but do not declare it
    onSuccess: async (saved?: unknown) => {
      await uploadPending((saved as { id?: number } | undefined)?.id);
      toast.success(t('bill.add.saved'));
      await invalidateCar(carId);
      await done();
    },
    onError: (error: unknown) => {
      toast.error(errorMessage(error, t('errors.backend')));
    },
  };
}
