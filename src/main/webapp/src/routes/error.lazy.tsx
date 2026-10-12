import { createLazyFileRoute } from '@tanstack/react-router';
import ErrorPage from '@/pages/ErrorPage';

export const Route = createLazyFileRoute('/error')({
  component: RouteComponent,
});

/** The backend redirects here when the login failed, with the reason of the provider */
function RouteComponent() {
  const reason = new URLSearchParams(window.location.search).get('reason');
  return <ErrorPage loginError={reason ?? undefined} />;
}
