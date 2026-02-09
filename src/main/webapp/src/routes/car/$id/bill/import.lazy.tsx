import { ImportBillPage } from '@/pages/car/bill/ImportBillPage';
import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/bill/import')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const navigate = Route.useNavigate();
  return <ImportBillPage id={Number(id)} navigate={navigate} />;
}
