import { createLazyFileRoute } from '@tanstack/react-router';
import RemindersPage from '@/pages/car/RemindersPage';

export const Route = createLazyFileRoute('/car/$id/reminders')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <RemindersPage carId={Number(id)} />;
}
