import { createFileRoute, redirect } from '@tanstack/react-router';
import CarLayout from '@/pages/car/CarLayout';

export const Route = createFileRoute('/car/$id')({
  beforeLoad: ({ params }) => {
    // Validate the car ID before any nested route loads
    const id = Number(params.id);

    if (!Number.isInteger(id) || id <= 0) {
      throw redirect({ to: '/car' });
    }

    return { id };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const { id } = Route.useRouteContext();
  return <CarLayout id={id} />;
}
