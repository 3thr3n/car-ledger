import { createLazyFileRoute } from '@tanstack/react-router';
import CarListPage from '@/pages/car/CarListPage';

export const Route = createLazyFileRoute('/car/')({
  component: Index,
});

function Index() {
  const navigate = Route.useNavigate();

  return <CarListPage navigate={navigate} />;
}
