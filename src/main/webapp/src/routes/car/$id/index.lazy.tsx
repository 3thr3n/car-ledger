import { createLazyFileRoute } from '@tanstack/react-router';
import CarViewPage from '@/pages/car/CarViewPage';

export const Route = createLazyFileRoute('/car/$id/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  const navigate = Route.useNavigate();

  return <CarViewPage id={id} navigate={navigate} />;
}
