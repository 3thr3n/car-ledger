import { createLazyFileRoute } from '@tanstack/react-router';
import CarUpdatePage from '@/pages/car/CarUpdatePage';

export const Route = createLazyFileRoute('/car/new')({
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();

  return <CarUpdatePage navigate={navigate} />;
}
