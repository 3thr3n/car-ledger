import { createLazyFileRoute } from '@tanstack/react-router';
import NotFoundPage from '@/pages/NotFoundPage';

export const Route = createLazyFileRoute('/dev/404')({
  component: NotFoundPage,
});
