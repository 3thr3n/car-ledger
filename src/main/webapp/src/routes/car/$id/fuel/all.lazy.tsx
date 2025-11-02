import Bill from '@/components/bill/Bills';
import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/fuel/all')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <Bill carId={Number(id)} />;
}
