import { createLazyFileRoute } from '@tanstack/react-router';
import CarUpdatePage from '@/pages/car/CarUpdatePage';

export const Route = createLazyFileRoute('/car/$id/edit')({
  component: EditRouteComponent,
});

function EditRouteComponent() {
  const { id } = Route.useParams();
  const navigate = Route.useNavigate();

  return <CarUpdatePage id={id} navigate={navigate} />;
}
