import { createLazyFileRoute } from '@tanstack/react-router';
import CarEditPage from '@/pages/car/CarEditPage';

export const Route = createLazyFileRoute('/car/$id/edit')({
  component: EditRouteComponent,
});

function EditRouteComponent() {
  const { id } = Route.useParams();
  const navigate = Route.useNavigate();

  return <CarEditPage id={id} navigate={navigate} />;
}
