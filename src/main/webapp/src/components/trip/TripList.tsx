import {
  Box,
  Button,
  Chip,
  IconButton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteOutlineRounded from '@mui/icons-material/DeleteOutlineRounded';
import FileDownloadRounded from '@mui/icons-material/FileDownloadRounded';
import RestoreRounded from '@mui/icons-material/RestoreRounded';
import RouteRounded from '@mui/icons-material/RouteRounded';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useTranslation } from 'react-i18next';
import useCarRole from '@/hooks/useCarRole';
import { Trip } from '@/generated';
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

const PAGE_SIZES = [10, 25, 50];

/** hidden on phones, the row shows them below the date instead */
const wide = { display: { xs: 'none', md: 'table-cell' } };

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
      {trip.destination && <Box>{trip.destination}</Box>}
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
  const total = data?.total ?? 0;

  return (
    <Section
      flush
      title={t('trip.list.title')}
      subtitle={t('trip.list.subtitle')}
      action={
        !!years?.length &&
        role('READ') && (
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
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small" data-testid="trip-list">
              <TableHead>
                <TableRow>
                  <TableCell>{t('trip.list.date')}</TableCell>
                  <TableCell sx={wide}>{t('trip.fields.type')}</TableCell>
                  <TableCell sx={wide}>{t('trip.list.route')}</TableCell>
                  <TableCell align="right">{t('trip.list.distance')}</TableCell>
                  <TableCell sx={wide}>{t('trip.list.details')}</TableCell>
                  <TableCell padding="checkbox" />
                </TableRow>
              </TableHead>
              <TableBody>
                {trips.map((trip) => (
                  <Fragment key={trip.id}>
                    <TableRow>
                      <TableCell>
                        <Stack
                          direction="row"
                          spacing={0.5}
                          sx={{ alignItems: 'center' }}
                        >
                          <span>{format.dateTime(trip.startedAt)}</span>
                          <CreatorHint name={trip.driver} />
                          {trip.restored && (
                            <Tooltip title={t('trip.list.restored')}>
                              <RestoreRounded
                                fontSize="small"
                                color="action"
                                aria-label={t('trip.list.restored')}
                              />
                            </Tooltip>
                          )}
                        </Stack>
                        <Box sx={{ display: { md: 'none' } }}>
                          <Typography
                            variant="body2"
                            sx={{ color: 'text.secondary' }}
                          >
                            {t(`trip.types.${trip.type}`)}
                          </Typography>
                          <Details trip={trip} />
                        </Box>
                      </TableCell>
                      <TableCell sx={wide}>
                        {t(`trip.types.${trip.type}`)}
                      </TableCell>
                      <TableCell
                        sx={{ ...wide, ...struck(trip), whiteSpace: 'nowrap' }}
                        className="tabular"
                      >
                        {format.distance(trip.startOdometer)} →{' '}
                        {trip.endOdometer == null
                          ? t('trip.list.open')
                          : format.distance(trip.endOdometer)}
                      </TableCell>
                      <TableCell
                        align="right"
                        className="tabular"
                        sx={struck(trip)}
                      >
                        {trip.distance == null
                          ? t('trip.list.open')
                          : format.distance(trip.distance)}
                      </TableCell>
                      <TableCell sx={wide}>
                        <Details trip={trip} />
                      </TableCell>
                      <TableCell padding="checkbox">
                        <Stack direction="row">
                          {styleUrl && (
                            <IconButton
                              size="small"
                              aria-label={t('trip.track.button')}
                              title={t('trip.track.button')}
                              aria-expanded={routeOf === trip.id}
                              color={
                                routeOf === trip.id ? 'primary' : 'default'
                              }
                              onClick={() =>
                                setRouteOf(
                                  routeOf === trip.id ? undefined : trip.id,
                                )
                              }
                            >
                              <RouteRounded fontSize="small" />
                            </IconButton>
                          )}
                          {!trip.deletedAt && role('WRITE') && (
                            <IconButton
                              size="small"
                              aria-label={t('trip.delete.button')}
                              title={t('trip.delete.button')}
                              onClick={() => setToDelete(trip)}
                            >
                              <DeleteOutlineRounded fontSize="small" />
                            </IconButton>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                    {styleUrl && routeOf === trip.id && (
                      <TableRow>
                        <TableCell colSpan={6} sx={{ py: 2 }}>
                          <TripTrack
                            carId={carId}
                            trip={trip}
                            styleUrl={styleUrl}
                          />
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          </Box>
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
