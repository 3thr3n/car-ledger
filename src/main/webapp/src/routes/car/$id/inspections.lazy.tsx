import { createLazyFileRoute } from '@tanstack/react-router';
import InspectionsPage from '@/pages/car/InspectionsPage';

export const Route = createLazyFileRoute('/car/$id/inspections')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <InspectionsPage carId={Number(id)} />;
}
