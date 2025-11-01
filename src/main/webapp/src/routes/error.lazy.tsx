import { createLazyFileRoute } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/error')({
  component: RouteComponent,
});

function RouteComponent() {
  return <div>Hello "/error"!</div>;
}
