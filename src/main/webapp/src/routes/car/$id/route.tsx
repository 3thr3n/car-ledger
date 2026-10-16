import { createFileRoute, redirect } from '@tanstack/react-router';
import { useEffect } from 'react';
import CarLayout from '@/pages/car/CarLayout';
import { rememberCar } from '@/utils/lastCar';

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
  useEffect(() => rememberCar(id), [id]);
  return <CarLayout id={id} />;
}
