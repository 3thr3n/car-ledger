import {
  Box,
  Chip,
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
import RestoreRounded from '@mui/icons-material/RestoreRounded';
import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
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

const PAGE_SIZES = [10, 25, 50];

/** hidden on phones, the row shows them below the date instead */
const wide = { display: { xs: 'none', md: 'table-cell' } };

function Details({ trip }: { trip: Trip }) {
  const { t } = useTranslation();
  return (
    <>
      {trip.destination && <Box>{trip.destination}</Box>}
      {(trip.purpose || trip.businessPartner) && (
        <Typography variant="body2" sx={{ color: 'text.secondary' }}>
          {[trip.purpose, trip.businessPartner].filter(Boolean).join(' · ')}
        </Typography>
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
                </TableRow>
              </TableHead>
              <TableBody>
                {trips.map((trip) => (
                  <TableRow key={trip.id}>
                    <TableCell>
                      <Stack
                        direction="row"
                        spacing={0.5}
                        sx={{ alignItems: 'center' }}
                      >
                        <span>{format.dateTime(trip.startedAt)}</span>
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
                      sx={{ ...wide, whiteSpace: 'nowrap' }}
                      className="tabular"
                    >
                      {format.distance(trip.startOdometer)} →{' '}
                      {trip.endOdometer == null
                        ? t('trip.list.open')
                        : format.distance(trip.endOdometer)}
                    </TableCell>
                    <TableCell align="right" className="tabular">
                      {trip.distance == null
                        ? t('trip.list.open')
                        : format.distance(trip.distance)}
                    </TableCell>
                    <TableCell sx={wide}>
                      <Details trip={trip} />
                    </TableCell>
                  </TableRow>
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
    </Section>
  );
}
