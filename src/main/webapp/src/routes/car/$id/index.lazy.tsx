import { createLazyFileRoute } from '@tanstack/react-router';
import CarOverviewPage from '@/pages/car/CarOverviewPage';

export const Route = createLazyFileRoute('/car/$id/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <CarOverviewPage id={Number(id)} />;
}
