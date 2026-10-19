import { createLazyFileRoute } from '@tanstack/react-router';
import TripsPage from '@/pages/car/TripsPage';

export const Route = createLazyFileRoute('/car/$id/trips')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <TripsPage carId={Number(id)} />;
}
