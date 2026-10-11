import { createLazyFileRoute } from '@tanstack/react-router';
import RecurringListPage from '@/pages/car/bill/RecurringListPage';

export const Route = createLazyFileRoute('/car/$id/bill/recurring/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <RecurringListPage carId={Number(id)} />;
}
