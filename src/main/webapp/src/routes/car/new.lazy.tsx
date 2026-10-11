import { createLazyFileRoute } from '@tanstack/react-router';
import CarFormPage from '@/pages/car/CarFormPage';

export const Route = createLazyFileRoute('/car/new')({
  component: () => <CarFormPage />,
});
