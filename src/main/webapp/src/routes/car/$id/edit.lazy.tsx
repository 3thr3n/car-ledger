import { createLazyFileRoute } from '@tanstack/react-router';
import CarFormPage from '@/pages/car/CarFormPage';

export const Route = createLazyFileRoute('/car/$id/edit')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <CarFormPage id={Number(id)} />;
}
