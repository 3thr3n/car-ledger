import { createLazyFileRoute } from '@tanstack/react-router';
import RecurringPage from '@/pages/car/bill/recurring/RecurringPage';

export const Route = createLazyFileRoute('/car/$id/bill/recurring/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <RecurringPage id="RecurringPage" carId={Number(id)} />;
}
