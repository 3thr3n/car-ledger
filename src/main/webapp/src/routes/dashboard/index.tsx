import { createFileRoute } from '@tanstack/react-router';
import DashboardPage, {
  DashboardPageSearch,
} from '@/pages/dashboard/DashboardPage';

export const Route = createFileRoute('/dashboard/')({
  component: RouteComponent,
});

function RouteComponent() {
  const navigate = Route.useNavigate();
  const search: DashboardPageSearch = Route.useSearch();

  const copiedSearch: DashboardPageSearch = {
    selectedCar: search.selectedCar,
    from: search.from,
    to: search.to,
  };

  return <DashboardPage navigate={navigate} search={{ ...copiedSearch }} />;
}
