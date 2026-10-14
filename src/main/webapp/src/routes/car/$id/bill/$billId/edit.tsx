import { createFileRoute, redirect } from '@tanstack/react-router';
import { BillType } from '@/generated';
import EditBillPage, { EditableBillType } from '@/pages/car/bill/EditBillPage';

const editableTypes: string[] = [
  BillType.FUEL,
  BillType.MAINTENANCE,
  BillType.MISCELLANEOUS,
  BillType.RECURRING,
];

export const Route = createFileRoute('/car/$id/bill/$billId/edit')({
  validateSearch: (
    search: Record<string, unknown>,
  ): { type?: EditableBillType } => ({
    type: editableTypes.includes(String(search.type))
      ? (search.type as EditableBillType)
      : undefined,
  }),
  beforeLoad: ({ params, search }) => {
    const billId = Number(params.billId);
    if (!Number.isInteger(billId) || billId <= 0 || !search.type) {
      throw redirect({ to: '/car/$id', params: { id: params.id } });
    }
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { id, billId } = Route.useParams();
  const { type } = Route.useSearch();

  return (
    <EditBillPage carId={Number(id)} billId={Number(billId)} type={type!} />
  );
}
