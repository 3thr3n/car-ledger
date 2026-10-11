import { createLazyFileRoute } from '@tanstack/react-router';
import GaragePage from '@/pages/car/GaragePage';

export const Route = createLazyFileRoute('/car/')({
  component: GaragePage,
});
