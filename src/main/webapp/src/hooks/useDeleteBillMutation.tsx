import { toast } from 'react-toastify';
import { deleteBillMutation } from '@/generated/@tanstack/react-query.gen';
import { useMutation } from '@tanstack/react-query';
import { localClient } from '@/utils/QueryClient';

export default function useDeleteBillMutation(
  carId: number,
  yearRefetch: () => void,
  billRefetch: () => void,
) {
  const { mutate } = useMutation({
    ...deleteBillMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Bill deleted!');
    },
    onSettled: async () => {
      await yearRefetch();
      await billRefetch();
    },
  });

  function onDelete(billId: number) {
    mutate({
      path: {
        carId,
        billId,
      },
    });
  }

  return { onDelete };
}
