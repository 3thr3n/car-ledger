import { createLazyFileRoute } from '@tanstack/react-router';
import { BillType } from '@/generated';
import BillListPage from '@/pages/car/bill/BillListPage';

export const Route = createLazyFileRoute('/car/$id/bill/miscellaneous/')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  return <BillListPage carId={Number(id)} type={BillType.MISCELLANEOUS} />;
}
