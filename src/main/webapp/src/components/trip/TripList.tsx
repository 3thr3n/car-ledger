import {
  Box,
  Button,
  Chip,
  IconButton,
  Stack,
  TablePagination,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import RestoreRounded from '@mui/icons-material/RestoreRounded';
import RouteRounded from '@mui/icons-material/RouteRounded';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import useCarRole from '@/hooks/useCarRole';
import { Trip, TripType } from '@/generated';
import {
  getTripsOptions,
  getTripYearsOptions,
} from '@/generated/@tanstack/react-query.gen';
import EmptyState from '@/components/ui/EmptyState';
import Section from '@/components/ui/Section';
import YearFilter from '@/components/ui/YearFilter';
import { useFormat } from '@/utils/format';
import { localClient } from '@/utils/QueryClient';
import TripDeleteDialog from './TripDeleteDialog';
import TripExportDialog from './TripExportDialog';
import TripTrack from './TripTrack';
import useTrackMap from '@/components/track/useTrackMap';
import CreatorHint from '@/components/ui/CreatorHint';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import { logDays } from './tripLog';

const PAGE_SIZES = [10, 25, 50];

/** the odometers of a deleted trip don't count */
const struck = (trip: Trip) =>
  trip.deletedAt
    ? { textDecoration: 'line-through', color: 'text.disabled' }
    : {};

function Details({ trip }: { trip: Trip }) {
  const { t } = useTranslation();
  const format = useFormat();
  return (
    <>
      {trip.deletedAt && (
        <Typography variant="body2" sx={{ color: 'error.main' }}>
          {t('trip.list.deleted', {
            date: format.dateTime(trip.deletedAt),
            reason: trip.deleteReason,
          })}
        </Typography>
      )}
      {(trip.purpose || trip.businessPartner) && (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {[trip.purpose, trip.businessPartner].filter(Boolean).join(' · ')}
        </Typography>
      )}
      {trip.enteredLater && (
        <Tooltip
          title={t('trip.list.enteredLaterHint', {
            time: format.dateTime(trip.finishedAt ?? trip.createdAt),
          })}
        >
          <Chip
            size="small"
            variant="outlined"
            label={t('trip.list.enteredLater')}
            sx={{ mt: 0.5, mr: 0.5 }}
          />
        </Tooltip>
      )}
      {trip.reviewReason && (
        <Tooltip title={t(`trip.review.${trip.reviewReason}`)}>
          <Chip
            size="small"
            color="warning"
            variant="outlined"
            label={t('trip.review.label')}
            sx={{ mt: 0.5 }}
          />
        </Tooltip>
      )}
    </>
  );
}

/** the column of the times, the one of the line, the text */
const COLUMNS = '4.75rem 1.5rem minmax(0, 1fr)';

/** the line of a trip by its type: business in the accent */
const lineColor = (type: TripType) =>
  ({
    [TripType.BUSINESS]: 'primary.main',
    [TripType.COMMUTE]: 'text.secondary',
    [TripType.PRIVATE]: 'text.disabled',
  })[type];

/** kilometers between two trips that no trip covers: a dotted warning */
function Gap({ km }: { km: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  return (
    <Box
      data-testid="trip-gap"
      sx={(theme) => ({
        display: 'grid',
        gridTemplateColumns: COLUMNS,
        alignItems: 'center',
        minHeight: 36,
        color: theme.vars.palette.warning.main,
        ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
      })}
    >
      <Box
        aria-hidden
        sx={{
          gridColumn: 2,
          justifySelf: 'center',
          alignSelf: 'stretch',
          borderLeft: '3px dotted currentColor',
        }}
      />
      <Typography
        variant="body2"
        sx={{ gridColumn: 3, pl: 1, fontWeight: 600, color: 'inherit' }}
      >
        {t('trip.list.gap', { distance: format.distance(km) })}
      </Typography>
    </Box>
  );
}

/** a trip as a part of the route of its day */
function Leg({
  trip,
  routeShown,
  onRoute,
  onDelete,
}: {
  trip: Trip;
  routeShown: boolean;
  /** shows or hides the GPS track below it, with a map only */
  onRoute?: () => void;
  onDelete?: () => void;
}) {
  const { t } = useTranslation();
  const format = useFormat();
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: COLUMNS,
        alignItems: 'stretch',
        minHeight: 64,
      }}
    >
      <Box
        className="tabular"
        sx={{
          pl: 0.5,
          py: 0.75,
          fontFamily: displayFont,
          lineHeight: 1.2,
          whiteSpace: 'nowrap',
          display: 'flex',
          flexDirection: 'column',
          justifyContent: 'space-between',
        }}
      >
        <Box component="span" sx={{ fontWeight: 650 }}>
          {format.time(trip.startedAt)}
        </Box>
        <Box component="span" sx={{ color: 'text.secondary' }}>
          {trip.endedAt ? format.time(trip.endedAt) : t('trip.list.open')}
        </Box>
      </Box>
      <Box
        aria-hidden
        sx={{
          justifySelf: 'center',
          my: 0.5,
          width: 6,
          borderRadius: 3,
          ...(trip.deletedAt
            ? {
                borderLeft: '3px dotted',
                borderColor: 'text.disabled',
                width: 0,
              }
            : { bgcolor: lineColor(trip.type) }),
        }}
      />
      {/* next to each other from a tablet on, wrapped below the text on a phone */}
      <Box
        sx={{
          pl: 1,
          py: 0.75,
          minWidth: 0,
          display: 'flex',
          flexWrap: { xs: 'wrap', sm: 'nowrap' },
          alignItems: 'flex-start',
          columnGap: 1.5,
        }}
      >
        <Box sx={{ flex: '1 1 14rem', minWidth: 0 }}>
          <Typography sx={{ fontWeight: 600 }}>
            {trip.destination ?? t(`trip.types.${trip.type}`)}
            <Typography
              component="span"
              variant="body2"
              sx={{ color: 'text.secondary', fontWeight: 400 }}
            >
              {trip.destination && ` · ${t(`trip.types.${trip.type}`)}`}
            </Typography>
            <CreatorHint name={trip.driver} />
            {trip.restored && (
              <Tooltip title={t('trip.list.restored')}>
                <RestoreRounded
                  fontSize="small"
                  color="action"
                  aria-label={t('trip.list.restored')}
                  sx={{ verticalAlign: 'middle', ml: 0.5 }}
                />
              </Tooltip>
            )}
          </Typography>
          <Details trip={trip} />
        </Box>
        <Box
          sx={{
            textAlign: { xs: 'left', sm: 'right' },
            whiteSpace: 'nowrap',
            ...struck(trip),
          }}
        >
          <Typography
            className="tabular"
            sx={{
              fontFamily: displayFont,
              fontWeight: 700,
              fontSize: '1.25rem',
              lineHeight: 1.2,
              color: 'inherit',
            }}
          >
            {trip.distance == null
              ? t('trip.list.open')
              : format.distance(trip.distance)}
          </Typography>
          <Typography
            className="tabular"
            variant="body2"
            sx={{ color: trip.deletedAt ? 'inherit' : 'text.secondary' }}
          >
            {format.distance(trip.startOdometer)} →{' '}
            {trip.endOdometer == null
              ? t('trip.list.open')
              : format.distance(trip.endOdometer)}
          </Typography>
        </Box>
        <Stack
          direction="row"
          sx={{
            alignItems: 'center',
            justifyContent: 'flex-end',
            // the same place for the distance with and without the buttons
            width: { sm: 80 },
            ml: { xs: 'auto', sm: 0 },
            flexShrink: 0,
          }}
        >
          {onRoute && (
            <IconButton
              aria-label={t('trip.track.button')}
              title={t('trip.track.button')}
              aria-expanded={routeShown}
              color={routeShown ? 'primary' : 'default'}
              onClick={onRoute}
            >
              <RouteRounded fontSize="small" />
            </IconButton>
          )}
          {onDelete && (
            <IconButton
              aria-label={t('trip.delete.button')}
              title={t('trip.delete.button')}
              onClick={onDelete}
            >
              <DeleteOutlineRounded fontSize="small" />
            </IconButton>
          )}
        </Stack>
      </Box>
    </Box>
  );
}

/** The trips of the car, the latest first, paged and filtered by year */
export default function TripList({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const [year, setYear] = useState<number>();
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(PAGE_SIZES[0]);
  const [toDelete, setToDelete] = useState<Trip>();
  const [exporting, setExporting] = useState(false);
  /** the trip whose GPS track is shown below it */
  const [routeOf, setRouteOf] = useState<string>();
  const styleUrl = useTrackMap(carId);
  const role = useCarRole(carId);

  const { data: years } = useQuery(
    getTripYearsOptions({ client: localClient, path: { carId } }),
  );
  const { data, isLoading } = useQuery({
    ...getTripsOptions({
      client: localClient,
      path: { carId },
      query: { page: page + 1, size, year },
    }),
    placeholderData: keepPreviousData,
  });
  const trips = data?.data ?? [];
  const dayName = (day: string) =>
    new Intl.DateTimeFormat(format.locale, {
      weekday: 'long',
      month: 'long',
      day: 'numeric',
      year: 'numeric',
    }).format(new Date(`${day}T12:00:00`));
  const total = data?.total ?? 0;

  return (
    <Section
      flush
      title={t('trip.list.title')}
      subtitle={t('trip.list.subtitle')}
      action={
        // a driver exports the own trips
        !!years?.length && (
          <Button
            size="small"
            startIcon={<FileDownloadRounded />}
            onClick={() => setExporting(true)}
          >
            {t('trip.export.button')}
          </Button>
        )
      }
    >
      <Box sx={{ px: 2.5, pb: 1.5 }}>
        <YearFilter
          years={years ?? []}
          value={year}
          onChange={(next) => {
            setYear(next);
            setPage(0);
          }}
        />
      </Box>
      {!isLoading && total === 0 ? (
        <EmptyState
          title={t('trip.list.emptyTitle')}
          text={t('trip.list.emptyText')}
        />
      ) : (
        <>
          <Stack
            spacing={3}
            data-testid="trip-list"
            sx={{ px: 2.5, pt: 1, pb: 1 }}
          >
            {logDays(trips).map(({ day, entries }) => (
              <Box component="section" key={day} aria-label={dayName(day)}>
                <Typography
                  component="h3"
                  sx={{
                    fontFamily: displayFont,
                    fontWeight: 650,
                    pb: 0.75,
                    mb: 0.5,
                    borderBottom: 1,
                    borderColor: 'divider',
                  }}
                >
                  {dayName(day)}
                </Typography>
                <Box component="ol" sx={{ listStyle: 'none', m: 0, p: 0 }}>
                  {entries.map(({ trip, gap }) => (
                    <Box component="li" key={trip.id} data-testid="trip">
                      {gap !== undefined && <Gap km={gap} />}
                      <Leg
                        trip={trip}
                        routeShown={routeOf === trip.id}
                        onRoute={
                          styleUrl
                            ? () =>
                                setRouteOf(
                                  routeOf === trip.id ? undefined : trip.id,
                                )
                            : undefined
                        }
                        onDelete={
                          !trip.deletedAt && role('WRITE')
                            ? () => setToDelete(trip)
                            : undefined
                        }
                      />
                      {styleUrl && routeOf === trip.id && (
                        <Box sx={{ py: 2 }}>
                          <TripTrack
                            carId={carId}
                            trip={trip}
                            styleUrl={styleUrl}
                          />
                        </Box>
                      )}
                    </Box>
                  ))}
                </Box>
              </Box>
            ))}
          </Stack>
          <TablePagination
            component="div"
            count={total}
            page={page}
            onPageChange={(_, next) => setPage(next)}
            rowsPerPage={size}
            rowsPerPageOptions={PAGE_SIZES}
            onRowsPerPageChange={(event) => {
              setSize(Number(event.target.value));
              setPage(0);
            }}
          />
        </>
      )}
      <TripDeleteDialog
        trip={toDelete}
        onClose={() => setToDelete(undefined)}
      />
      <TripExportDialog
        carId={carId}
        years={years ?? []}
        year={year}
        open={exporting}
        onClose={() => setExporting(false)}
      />
    </Section>
  );
}
