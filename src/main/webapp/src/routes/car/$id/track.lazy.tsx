import { createLazyFileRoute } from '@tanstack/react-router';
import TrackPage from '@/pages/car/TrackPage';

export const Route = createLazyFileRoute('/car/$id/track')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <TrackPage carId={Number(id)} />;
}
