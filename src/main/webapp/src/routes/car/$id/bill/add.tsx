import { createFileRoute } from '@tanstack/react-router';
import { BillType } from '@/generated';
import { isBillType } from '@/utils/billTypes';
import AddBillPage from '@/pages/car/bill/AddBillPage';

export const Route = createFileRoute('/car/$id/bill/add')({
  validateSearch: (search: Record<string, unknown>): { type?: BillType } => ({
    type: isBillType(search.type) ? search.type : undefined,
  }),
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const { type } = Route.useSearch();
  const navigate = Route.useNavigate();

  return (
    <AddBillPage
      carId={Number(id)}
      type={type ?? BillType.FUEL}
      onTypeChange={(next) =>
        navigate({ search: { type: next }, replace: true })
      }
    />
  );
}
