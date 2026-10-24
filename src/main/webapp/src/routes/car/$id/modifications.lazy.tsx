import { createLazyFileRoute } from '@tanstack/react-router';
import ModificationsPage from '@/pages/car/ModificationsPage';

export const Route = createLazyFileRoute('/car/$id/modifications')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <ModificationsPage carId={Number(id)} />;
}
