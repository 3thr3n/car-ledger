import AllFuelPage from '@/pages/car/bill/fuel/FuelPage';
import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/bill/fuel/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <AllFuelPage id="AllFuelPage" carId={Number(id)} />;
}
