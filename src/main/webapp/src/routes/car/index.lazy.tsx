import { createLazyFileRoute } from '@tanstack/react-router';
import CarGridList from '@/components/carGrid/CarGridList';

export const Route = createLazyFileRoute('/car/')({
  component: Index,
});

function Index() {
  return <CarGridList />;
}
