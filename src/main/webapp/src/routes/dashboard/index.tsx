import { createFileRoute } from '@tanstack/react-router';
import DashboardPage from '@/pages/dashboard/DashboardPage';
import { DashboardSearch } from '@/components/dashboard/DashboardFilter';

const isApiDate = (value: unknown): value is string =>
  typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value);

export const Route = createFileRoute('/dashboard/')({
  validateSearch: (search: Record<string, unknown>): DashboardSearch => {
    const car = Number(search.car);
    return {
      car: Number.isInteger(car) && car > 0 ? car : undefined,
      from: isApiDate(search.from) ? search.from : undefined,
      to: isApiDate(search.to) ? search.to : undefined,
    };
  },
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const search = Route.useSearch();

  return (
    <DashboardPage
      search={search}
      onSearchChange={(next) => navigate({ search: next, replace: true })}
    />
  );
}
