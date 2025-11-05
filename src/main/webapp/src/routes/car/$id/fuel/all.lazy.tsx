import AllFuelPage from '@/pages/car/fuel/AllFuelPage';
import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/fuel/all')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();

  return (
    <AllFuelPage
      id={Number(id)}
      onDelete={() => console.log('*hust* Delete')}
    />
  );
}
