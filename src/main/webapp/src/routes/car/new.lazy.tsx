import { createLazyFileRoute } from '@tanstack/react-router';
import CarNewPage from '@/pages/car/CarNewPage';

export const Route = createLazyFileRoute('/car/new')({
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();

  return <CarNewPage navigate={navigate} />;
}
