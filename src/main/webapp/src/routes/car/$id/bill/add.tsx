import { createFileRoute } from '@tanstack/react-router';
import { BillType } from '@/generated';
import { isBillType } from '@/utils/billTypes';
import AddBillPage from '@/pages/car/bill/AddBillPage';

export const Route = createFileRoute('/car/$id/bill/add')({
  validateSearch: (
    search: Record<string, unknown>,
  ): { type?: BillType; queued?: string } => ({
    type: isBillType(search.type) ? search.type : undefined,
    // a bill of the offline queue to correct
    queued: typeof search.queued === 'string' ? search.queued : undefined,
  }),
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const { type, queued } = Route.useSearch();
  const navigate = Route.useNavigate();

  return (
    <AddBillPage
      carId={Number(id)}
      type={type}
      queued={queued}
      onTypeChange={(next) =>
        navigate({ search: { type: next }, replace: true })
      }
    />
  );
}
