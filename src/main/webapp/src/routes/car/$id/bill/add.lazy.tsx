import { createLazyFileRoute } from '@tanstack/react-router';
import AddBillPage from '@/pages/car/bill/AddBillPage';
import { BillType } from '@/generated';

export const Route = createLazyFileRoute('/car/$id/bill/add')({
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useParams();
  let { type }: { type: BillType | undefined } = Route.useSearch();
  const nav = Route.useNavigate();

  if (type && !Object.values(BillType).includes(type)) {
    type = undefined;
    nav({
      replace: true,
      search: {},
    });
  }

  return <AddBillPage id={Number(id)} type={type} />;
}
