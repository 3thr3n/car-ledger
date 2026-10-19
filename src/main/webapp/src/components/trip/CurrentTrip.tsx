import { Alert, Box, Button, Skeleton, Stack, Typography } from '@mui/material';
import CloudOffRounded from '@mui/icons-material/CloudOffRounded';
import FlagRounded from '@mui/icons-material/FlagRounded';
import PlayArrowRounded from '@mui/icons-material/PlayArrowRounded';
import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { getMyCarOptions } from '@/generated/@tanstack/react-query.gen';
import EmptyState from '@/components/ui/EmptyState';
import Section from '@/components/ui/Section';
import { RouterButton } from '@/components/ui/links';
import { useFormat } from '@/utils/format';
import { localClient } from '@/utils/QueryClient';
import { TripFinishDialog, TripStartDialog } from './TripDialogs';
import { useTripState } from './useTrip';

/**
 * The open trip of the car with "Finish trip", otherwise "Start trip". Works
 * without a connection with the state kept on the device. With the trip log
 * turned off no trip starts, an open one can still be finished.
 */
export default function CurrentTrip({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const state = useTripState(carId);
  const [starting, setStarting] = useState(false);
  const [finishing, setFinishing] = useState(false);
  const { open } = state;
  // loaded by the layout of the car already
  const { data: car } = useQuery(
    getMyCarOptions({ client: localClient, path: { id: carId } }),
  );

  if (car && !car.tripLog && !open) {
    return (
      <Section>
        <EmptyState
          title={t('trip.off.title')}
          text={t('trip.off.text')}
          action={
            <RouterButton
              to="/car/$id/edit"
              params={{ id: `${carId}` }}
              variant="contained"
            >
              {t('trip.off.turnOn')}
            </RouterButton>
          }
        />
      </Section>
    );
  }

  return (
    <Section title={t('trip.current.title')}>
      <Box data-testid="current-trip">
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          sx={{ alignItems: { xs: 'stretch', sm: 'center' }, pb: 2 }}
        >
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            {state.isLoading ? (
              <Skeleton width={240} />
            ) : open ? (
              <>
                <Typography sx={{ fontWeight: 600 }}>
                  {t('trip.current.open')} · {t(`trip.types.${open.type}`)}
                  {open.destination && ` · ${open.destination}`}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('trip.current.since', {
                    time: format.dateTime(open.startedAt),
                    odometer: format.distance(open.startOdometer),
                  })}
                </Typography>
              </>
            ) : (
              <>
                <Typography sx={{ fontWeight: 600 }}>
                  {t('trip.current.none')}
                </Typography>
                <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                  {t('trip.current.noneText')}
                </Typography>
              </>
            )}
          </Box>
          {open ? (
            <Button
              variant="contained"
              size="large"
              startIcon={<FlagRounded />}
              onClick={() => setFinishing(true)}
            >
              {t('trip.finish.button')}
            </Button>
          ) : (
            <Button
              variant="contained"
              size="large"
              startIcon={<PlayArrowRounded />}
              disabled={state.isLoading}
              onClick={() => setStarting(true)}
            >
              {t('trip.start.button')}
            </Button>
          )}
        </Stack>
        {open?.pending && (
          <Alert
            severity="info"
            variant="outlined"
            icon={<CloudOffRounded />}
            sx={{ mb: 2 }}
          >
            {t('trip.current.pending')}
          </Alert>
        )}
      </Box>

      <TripStartDialog
        carId={carId}
        open={starting}
        minimumOdometer={state.minimumOdometer}
        suggestedOdometer={state.suggestedOdometer}
        onClose={() => setStarting(false)}
      />
      <TripFinishDialog
        carId={carId}
        trip={finishing ? open : undefined}
        onClose={() => setFinishing(false)}
      />
    </Section>
  );
}
