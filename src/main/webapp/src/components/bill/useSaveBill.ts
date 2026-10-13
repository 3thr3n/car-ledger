import { useNavigate } from '@tanstack/react-router';
import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { BillType } from '@/generated';
import { invalidateCar } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import { billTypes } from '@/utils/billTypes';
import { uploadAttachment } from './attachments';
import { BillAttachmentsProps } from './BillAttachments';

/**
 * Shared success/error handling of the "add bill" mutations.
 * `again` keeps the user on the form for the next receipt.
 * The files picked for a new bill are attached once it is saved.
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
            reason:
              error instanceof BackendError && error.message
                ? error.message
                : t('errors.backend'),
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

  return {
    attachments,
    markAgain: (value: boolean) => {
      again.current = value;
    },
    // the create endpoints answer with the saved bill, but do not declare it
    onSuccess: async (saved?: unknown) => {
      await uploadPending((saved as { id?: number } | undefined)?.id);
      toast.success(t('bill.add.saved'));
      await invalidateCar(carId);
      if (again.current) {
        reset();
      } else {
        await navigate({
          to: billTypes[type].listRoute,
          params: { id: `${carId}` },
        });
      }
    },
    onError: (error: Error) => {
      toast.error(
        error instanceof BackendError && error.message
          ? error.message
          : t('errors.backend'),
      );
    },
  };
}
