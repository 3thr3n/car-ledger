import { createLazyFileRoute } from '@tanstack/react-router';
import AddBillPage from '@/pages/car/bill/AddBillPage';

export const Route = createLazyFileRoute('/car/$id/bill/add')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <AddBillPage id={Number(id)} />;
}
