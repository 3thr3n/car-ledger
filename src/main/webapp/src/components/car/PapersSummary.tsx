import { Box, Chip, Divider, Stack, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';
import dayjs from 'dayjs';
import { useTranslation } from 'react-i18next';
import { useFormat } from '@/utils/format';
import useCarRole from '@/hooks/useCarRole';
import Section from '@/components/ui/Section';
import EmptyState from '@/components/ui/EmptyState';
import { RouterButton, RouterLink } from '@/components/ui/links';
import InspectionResultChip from '@/components/inspection/InspectionResultChip';
import { registrationOpen } from '@/components/modification/approval';
import {
  inspectionsOptions,
  installedOptions,
} from '@/components/offline/useOfflineDocuments';

/** an HU is due soon this many days before the end of its month */
const SOON_DAYS = 30;

/**
 * The next HU of the car and what is open with its modifications, from the
 * lists of the tabs HU and Modifications
 */
export default function PapersSummary({ carId }: { carId: number }) {
  const { t } = useTranslation();
  const format = useFormat();
  const params = { id: `${carId}` };
  const canChange = useCarRole(carId)('WRITE');
  const { data: inspections } = useQuery(inspectionsOptions(carId));
  const { data: modifications } = useQuery(installedOptions(carId));

  if (!inspections || !modifications) return null;
  const latest = inspections[0];
  const inspectionOpen = modifications.filter((each) => each.inspectionOpen);
  const unregistered = modifications.filter(registrationOpen);

  if (!latest && !modifications.length) {
    return (
      <Section title={t('car.overview.papers.title')}>
        <EmptyState
          compact
          title={t('car.overview.papers.empty')}
          action={
            canChange && (
              <RouterButton
                to="/car/$id/inspections"
                params={params}
                size="small"
                variant="outlined"
              >
                {t('inspection.add.button')}
              </RouterButton>
            )
          }
        />
      </Section>
    );
  }

  // due until the end of its month
  const due = latest?.nextDue ? dayjs(latest.nextDue).endOf('month') : null;
  const daysLeft = due?.diff(dayjs(), 'day');
  const month = (date: string) =>
    new Intl.DateTimeFormat(format.locale, {
      month: 'long',
      year: 'numeric',
    }).format(dayjs(date).toDate());

  return (
    <Section title={t('car.overview.papers.title')}>
      <Stack spacing={1.5} divider={<Divider />} data-testid="papers-summary">
        <Box>
          <Stack
            direction="row"
            spacing={1}
            useFlexGap
            sx={{ alignItems: 'center', flexWrap: 'wrap' }}
          >
            <RouterLink
              to="/car/$id/inspections"
              params={params}
              underline="hover"
              sx={{ fontWeight: 600 }}
            >
              {latest?.nextDue
                ? t('inspection.nextDue', { month: month(latest.nextDue) })
                : t('car.overview.papers.noNextDue')}
            </RouterLink>
            {daysLeft !== undefined && daysLeft < 0 && (
              <Chip
                size="small"
                color="error"
                label={t('reminder.status.OVERDUE')}
                sx={{ fontWeight: 600 }}
              />
            )}
            {daysLeft !== undefined &&
              daysLeft >= 0 &&
              daysLeft <= SOON_DAYS && (
                <Chip
                  size="small"
                  color="warning"
                  label={t('reminder.status.DUE_SOON')}
                  sx={{ fontWeight: 600 }}
                />
              )}
          </Stack>
          {latest && (
            <Stack
              direction="row"
              spacing={1}
              sx={{ alignItems: 'center', mt: 0.75 }}
            >
              <Typography variant="body2" sx={{ color: 'text.secondary' }}>
                {t('car.overview.papers.latest', {
                  date: format.date(latest.date),
                })}
              </Typography>
              <InspectionResultChip result={latest.result} />
            </Stack>
          )}
        </Box>
        <Box>
          <RouterLink
            to="/car/$id/modifications"
            params={params}
            underline="hover"
            sx={{ fontWeight: 600 }}
          >
            {t('car.overview.papers.installed', {
              count: modifications.length,
            })}
          </RouterLink>
          {(inspectionOpen.length > 0 || unregistered.length > 0) && (
            <Stack
              direction="row"
              spacing={1}
              useFlexGap
              sx={{ flexWrap: 'wrap', mt: 0.75 }}
            >
              {inspectionOpen.length > 0 && (
                <Chip
                  size="small"
                  color="warning"
                  label={t('car.overview.papers.inspectionOpen', {
                    count: inspectionOpen.length,
                  })}
                  sx={{ fontWeight: 600 }}
                />
              )}
              {unregistered.length > 0 && (
                <Chip
                  size="small"
                  color="info"
                  variant="outlined"
                  label={t('car.overview.papers.unregistered', {
                    count: unregistered.length,
                  })}
                />
              )}
            </Stack>
          )}
        </Box>
      </Stack>
    </Section>
  );
}
