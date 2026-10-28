import { createLazyFileRoute } from '@tanstack/react-router';
import NotificationsPage from '@/pages/NotificationsPage';

export const Route = createLazyFileRoute('/notifications')({
  component: NotificationsPage,
});
