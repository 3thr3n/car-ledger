import MaintenancePage from '@/pages/car/bill/maintenance/MaintenancePage';
import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/bill/maintenance/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <MaintenancePage id="MaintenancePage" carId={Number(id)} />;
}
