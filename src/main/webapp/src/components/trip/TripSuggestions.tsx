import { Box, Button, Divider, Stack, Typography } from '@mui/material';
import RouteRounded from '@mui/icons-material/RouteRounded';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { toast } from 'react-toastify';
import useCarRole from '@/hooks/useCarRole';
import { TripSuggestion } from '@/generated';
import {
  dismissTripSuggestionMutation,
  getTripSuggestionsOptions,
  getTripSuggestionsQueryKey,
} from '@/generated/@tanstack/react-query.gen';
import queryClient, { localClient } from '@/utils/QueryClient';
import { errorMessage } from '@/utils/BackendError';
import { useFormat } from '@/utils/format';
import Section from '@/components/ui/Section';
import TripPastDialog from './TripPastDialog';

/**
 * Drives of the GPS tracks without a trip, e.g. one not started in time. GPS
 * never writes a trip: each is added as a past trip, prefilled, or dismissed.
 */
export default function TripSuggestions({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const [adding, setAdding] = useState<TripSuggestion>();
  // the server only suggests to whom may dismiss them: not a viewer or a driver
  const canChange = useCarRole(carId)('WRITE');
  const { data: suggestions } = useQuery({
    ...getTripSuggestionsOptions({ client: localClient, path: { carId } }),
    enabled: canChange,
  });

  const dismiss = useMutation({
    ...dismissTripSuggestionMutation({ client: localClient }),
    onSuccess: async () => {
      toast.success(t('trip.suggestions.dismissed'));
      await queryClient.invalidateQueries({
        queryKey: getTripSuggestionsQueryKey({ path: { carId } }),
      });
    },
    onError: (e) => toast.error(errorMessage(e, t('errors.backend'))),
  });

  if (!suggestions?.length) return null;

  return (
    <Section
      title={t('trip.suggestions.title')}
      subtitle={t('trip.suggestions.subtitle')}
    >
      <Box sx={{ pb: 1 }} data-testid="trip-suggestions">
        {suggestions.map((suggestion, index) => (
          <Fragment key={suggestion.startedAt}>
            {index > 0 && <Divider />}
            <SuggestionRow
              suggestion={suggestion}
              disabled={dismiss.isPending}
              onAdd={() => setAdding(suggestion)}
              onDismiss={() =>
                dismiss.mutate({
                  path: { carId },
                  body: {
                    startedAt: suggestion.startedAt,
                    endedAt: suggestion.endedAt,
                  },
                })
              }
            />
          </Fragment>
        ))}
      </Box>
      <TripPastDialog
        carId={carId}
        open={adding !== undefined}
        initial={adding}
        onClose={() => setAdding(undefined)}
      />
    </Section>
  );
}

function SuggestionRow({
  suggestion,
  disabled,
  onAdd,
  onDismiss,
}: {
  suggestion: TripSuggestion;
  disabled: boolean;
  onAdd: () => void;
  onDismiss: () => void;
}) {
  const { t } = useTranslation();
  const format = useFormat();

  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={{ xs: 1, sm: 2 }}
      sx={{ alignItems: { sm: 'center' }, py: 1.5 }}
      data-testid="trip-suggestion"
    >
      <Stack
        direction="row"
        spacing={1.5}
        sx={{ alignItems: 'center', flexGrow: 1, minWidth: 0 }}
      >
        <RouteRounded sx={{ color: 'text.secondary' }} />
        <Box sx={{ minWidth: 0 }}>
          <Typography sx={{ fontWeight: 500 }}>
            {t('trip.suggestions.when', {
              start: format.dateTime(suggestion.startedAt),
              end: format.dateTime(suggestion.endedAt),
            })}
          </Typography>
          <Typography variant="body2" sx={{ color: 'text.secondary' }}>
            {t('trip.suggestions.distance', {
              distance: format.distance(suggestion.distance, 1),
            })}
          </Typography>
        </Box>
      </Stack>
      <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
        <Button size="small" variant="outlined" onClick={onAdd}>
          {t('trip.suggestions.add')}
        </Button>
        <Button
          size="small"
          color="inherit"
          onClick={onDismiss}
          disabled={disabled}
        >
          {t('trip.suggestions.dismiss')}
        </Button>
      </Stack>
    </Stack>
  );
}
