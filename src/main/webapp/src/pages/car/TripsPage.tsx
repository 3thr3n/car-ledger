import { Stack } from '@mui/material';
import CurrentTrip from '@/components/trip/CurrentTrip';
import PendingTrips from '@/components/trip/PendingTrips';
import TripList from '@/components/trip/TripList';

/**
 * The trip log of the car (Fahrtenbuch): trips are started and finished, not
 * edited. Starting and finishing also work without a connection.
 */
export default function TripsPage({ carId }: { carId: number }) {
  return (
    <Stack spacing={3}>
      <CurrentTrip carId={carId} />
      <PendingTrips carId={carId} />
      <TripList carId={carId} />
    </Stack>
  );
}
