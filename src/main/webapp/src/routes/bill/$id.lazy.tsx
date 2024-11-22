import Bill from '@/components/bill/Bills';
import { Typography } from '@mui/material';
import { createLazyFileRoute, useNavigate } from '@tanstack/react-router';

export const Route = createLazyFileRoute('/bill/$id')({
  component: BillRoute,
  errorComponent: BillError,
});

function BillRoute() {
  const { id: carId }: { id: string } = Route.useParams();
  const navigation = useNavigate();

  const id = parseInt(carId);

  if (Number.isNaN(id)) {
    console.error('id is not a number!');
    navigation({ to: '/' });
    return;
  }

  return <Bill carId={id} />;
}

function BillError() {
  return (
    <Typography variant="h2" content="div">
      Failed to load Bills
    </Typography>
  );
}
