import { createLazyFileRoute } from '@tanstack/react-router';
import TrackPage from '@/pages/car/TrackPage';

export const Route = createLazyFileRoute('/car/$id/track')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const search = Route.useSearch();
  const navigate = Route.useNavigate();
  return (
    <TrackPage
      carId={Number(id)}
      search={search}
      onSearchChange={(next) => navigate({ search: next, replace: true })}
    />
  );
}
