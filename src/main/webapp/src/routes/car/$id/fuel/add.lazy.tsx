import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/car/$id/fuel/add')({
  component: RouteComponent,
});

function RouteComponent() {
  return <div>Hello "/car/$id/fuel/add"!</div>;
}
