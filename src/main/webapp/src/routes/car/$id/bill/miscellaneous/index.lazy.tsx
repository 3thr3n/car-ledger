import { createLazyFileRoute } from '@tanstack/react-router';
import MiscellaneousPage from '@/pages/car/bill/miscellaneous/MiscellaneousPage';

export const Route = createLazyFileRoute('/car/$id/bill/miscellaneous/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <MiscellaneousPage id="MiscellaneousPage" carId={Number(id)} />;
}
