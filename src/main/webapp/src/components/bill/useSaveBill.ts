import { useNavigate } from '@tanstack/react-router';
import { useRef } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import { BillType } from '@/generated';
import { invalidateCar } from '@/utils/QueryClient';
import { BackendError } from '@/utils/BackendError';
import { billTypes } from '@/utils/billTypes';

/**
 * Shared success/error handling of the "add bill" mutations.
 * `again` keeps the user on the form for the next receipt.
 */
export default function useSaveBill(
  carId: number,
  type: BillType,
  reset: () => void,
) {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const again = useRef(false);

  return {
    markAgain: (value: boolean) => {
      again.current = value;
    },
    onSuccess: async () => {
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
