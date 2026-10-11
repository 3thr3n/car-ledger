import { createFileRoute } from '@tanstack/react-router';
import { BillType } from '@/generated';
import { isBillType } from '@/utils/billTypes';
import ImportBillPage from '@/pages/car/bill/ImportBillPage';

export const Route = createFileRoute('/car/$id/bill/import')({
  validateSearch: (search: Record<string, unknown>): { type?: BillType } => ({
    type: isBillType(search.type) ? search.type : undefined,
  }),
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const { type } = Route.useSearch();

  return <ImportBillPage carId={Number(id)} initialType={type} />;
}
