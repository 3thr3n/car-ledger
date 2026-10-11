import { useMutation } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import {
  deleteBillMutation,
  deleteRecurringBillMutation,
} from '@/generated/@tanstack/react-query.gen';
import { invalidateCar, localClient } from '@/utils/QueryClient';

/**
 * Recurring bills have an own delete endpoint, all other bills share one
 */
export default function useDeleteBill(carId: number, recurring = false) {
  const { t } = useTranslation();

  const options = {
    onSuccess: () => {
      toast.success(t('bill.delete.done'));
    },
    onError: () => {
      toast.error(t('bill.delete.failed'));
    },
    onSettled: () => invalidateCar(carId),
  };

  const bill = useMutation({
    ...deleteBillMutation({ client: localClient }),
    ...options,
  });
  const recurringBill = useMutation({
    ...deleteRecurringBillMutation({ client: localClient }),
    ...options,
  });

  return (billId: number) =>
    recurring
      ? recurringBill.mutate({ path: { carId, billId } })
      : bill.mutate({ path: { carId, billId } });
}
