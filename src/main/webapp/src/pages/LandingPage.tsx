import { Alert, Box, Button, Container, Typography } from '@mui/material';
import { ReactNode, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import useMe from '@/hooks/useMe';
import {
  forgetAccountDeleted,
  wasAccountDeleted,
} from '@/utils/accountDeleted';
import { baseUrl } from '@/utils/QueryClient';
import { useFormat } from '@/utils/format';
import { scaleOf } from '@/utils/gauge';
import { RouterButton } from '@/components/ui/links';
import { CarPlate } from '@/components/layout/CarAvatar';
import Gauge from '@/components/car/Gauge';
import { BillPaletteKey } from '@/utils/billTypes';
import { CLUSTER_AMBER, displayFont } from '@/theme';
import { SOURCE_URL } from '@/utils/source';
import LegalLinks from '@/components/legal/LegalLinks';

interface Feature {
  key: string;
  /** a kind of entry keeps the color of its lists */
  color?: BillPaletteKey;
}

/** the features in the groups a driver thinks in */
const groups: { key: string; features: Feature[] }[] = [
  {
    key: 'costs',
    features: [
      { key: 'fuel', color: 'fuel' },
      { key: 'charging', color: 'charging' },
      { key: 'maintenance', color: 'maintenance' },
      { key: 'recurring', color: 'recurring' },
      { key: 'miscellaneous', color: 'miscellaneous' },
      { key: 'stats' },
    ],
  },
  {
    key: 'papers',
    features: [
      { key: 'inspections' },
      { key: 'modifications' },
      { key: 'papers' },
      { key: 'reminders' },
    ],
  },
  {
    key: 'road',
    features: [{ key: 'trips' }, { key: 'gps' }, { key: 'sharing' }],
  },
  {
    key: 'data',
    features: [{ key: 'offline' }, { key: 'import' }, { key: 'backup' }],
  },
];

/** an example car of a year, in the units and the currency shown */
const example = {
  total: 2589.76,
  costPerKm: 13.93,
  perMonth: 215.81,
  consumption: { value: 6.01, low: 4.97, high: 7.35, last: 4.97 },
  distance: { value: 1549, low: 1319, high: 2158, last: 1493 },
};

function Readout({ label, value }: { label: ReactNode; value: ReactNode }) {
  return (
    <Box sx={{ textAlign: 'center', minWidth: 0 }}>
      <Typography
        component="dt"
        variant="body2"
        sx={{ color: 'text.secondary' }}
      >
        {label}
      </Typography>
      <Typography
        component="dd"
        className="tabular"
        sx={{
          m: 0,
          fontFamily: displayFont,
          fontWeight: 650,
          fontSize: '1.25rem',
          whiteSpace: 'nowrap',
        }}
      >
        {value}
      </Typography>
    </Box>
  );
}

/**
 * What the page of a car looks like: the cluster of an example car, built from
 * the parts of the app
 */
function ExampleCluster() {
  const { t } = useTranslation();
  const format = useFormat();
  const { consumption, distance } = example;
  return (
    <Box
      component="figure"
      sx={{ m: 0, minWidth: 0 }}
      aria-labelledby="example-caption"
    >
      <Box
        sx={(theme) => ({
          p: { xs: 2.5, sm: 3 },
          bgcolor: 'background.paper',
          border: 1,
          borderColor: 'divider',
          borderRadius: '12px',
          ...theme.applyStyles('dark', {
            border: 0,
            borderRadius: '28px',
            background: `radial-gradient(120% 90% at 50% 0%, #1c2836 0%, ${theme.vars.palette.background.paper} 70%)`,
            boxShadow: `inset 0 0 0 1px ${theme.vars.palette.divider}`,
          }),
        })}
      >
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            gap: 1.25,
            mb: 2.5,
          }}
        >
          <CarPlate id={1} text="M-OC 2019" size={28} />
          <Typography sx={{ fontWeight: 600 }}>
            {t('landing.example.car')}
          </Typography>
        </Box>
        <Box sx={{ textAlign: 'center' }}>
          <Typography sx={{ color: 'text.secondary' }}>
            {t('car.cluster.spent')}
          </Typography>
          <Typography
            component="p"
            className="tabular"
            sx={(theme) => ({
              m: 0,
              fontFamily: displayFont,
              fontWeight: 700,
              fontSize: 'clamp(2.5rem, 2rem + 2vw, 3.5rem)',
              lineHeight: 1.05,
              ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
            })}
          >
            {format.money(example.total)}
          </Typography>
          <Box
            component="dl"
            sx={{
              m: 0,
              mt: 1,
              display: 'inline-grid',
              gridTemplateColumns: 'repeat(2, auto)',
              columnGap: 4,
            }}
          >
            <Readout
              label={t('dashboard.summary.costPerKm', {
                context: format.units.distance.context,
              })}
              value={format.costPerDistance(example.costPerKm)}
            />
            <Readout
              label={t('car.cluster.perMonth')}
              value={format.money(example.perMonth)}
            />
          </Box>
        </Box>
        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: 'repeat(2, minmax(0, 1fr))',
            columnGap: 2,
            mt: 2.5,
          }}
        >
          <Gauge
            {...scaleOf(
              [consumption.low, consumption.high, consumption.value],
              1,
            )}
            value={consumption.value}
            low={consumption.low}
            high={consumption.high}
            last={consumption.last}
            text={format.number(consumption.value, 2)}
            unit="l/100km"
            label={t('car.cluster.consumptionLabel', {
              value: format.consumption(consumption.value),
            })}
          />
          <Gauge
            {...scaleOf([distance.low, distance.high, distance.value], 500)}
            value={distance.value}
            low={distance.low}
            high={distance.high}
            last={distance.last}
            text={format.number(distance.value, 0)}
            unit={t('car.cluster.perMonthUnit', { unit: 'km' })}
            label={t('car.cluster.distanceLabel', {
              value: format.distance(distance.value),
            })}
          />
        </Box>
      </Box>
      <Typography
        id="example-caption"
        component="figcaption"
        variant="body2"
        sx={{ color: 'text.secondary', mt: 1.25, textAlign: 'center' }}
      >
        {t('landing.example.caption')}
      </Typography>
    </Box>
  );
}

export default function LandingPage() {
  const { t } = useTranslation();
  const { loggedIn } = useMe();

  // read once, a toast would fire before the toast container is ready
  const [accountDeleted] = useState(wasAccountDeleted);
  useEffect(forgetAccountDeleted, []);

  return (
    <Box id="LandingPage" sx={{ py: { xs: 4, md: 8 } }}>
      <Container maxWidth="lg">
        {accountDeleted && !loggedIn && (
          <Alert severity="success" sx={{ mb: 4 }}>
            {t('settings.deleteAccount.done')}
          </Alert>
        )}

        <Box
          component="section"
          sx={{
            display: 'grid',
            gridTemplateColumns: {
              xs: '1fr',
              md: 'minmax(0, 1fr) minmax(0, 1.05fr)',
            },
            columnGap: 8,
            rowGap: 5,
            alignItems: 'center',
            mb: { xs: 8, md: 12 },
          }}
        >
          <Box sx={{ minWidth: 0 }}>
            <Typography
              variant="h2"
              component="h1"
              sx={{
                fontSize: { xs: '2.5rem', md: '3.75rem' },
                lineHeight: 1.05,
                textWrap: 'balance',
                mb: 3,
              }}
            >
              {t('landing.title')}
            </Typography>
            <Typography
              sx={{
                color: 'text.secondary',
                fontSize: '1.125rem',
                maxWidth: '34rem',
                mb: 4,
              }}
            >
              {t('landing.subtitle')}
            </Typography>
            {loggedIn ? (
              <RouterButton to="/dashboard" variant="contained" size="large">
                {t('landing.ctaDashboard')}
              </RouterButton>
            ) : (
              <Button
                href={`${baseUrl}/api/auth/login`}
                variant="contained"
                size="large"
              >
                {t('landing.cta')}
              </Button>
            )}
            <Typography
              variant="body2"
              sx={{ color: 'text.secondary', mt: 2, maxWidth: '34rem' }}
            >
              {t('landing.selfHosted')}
            </Typography>
          </Box>
          <ExampleCluster />
        </Box>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', md: 'repeat(2, minmax(0, 1fr))' },
            columnGap: 8,
            rowGap: 7,
          }}
        >
          {groups.map((group) => (
            <Box
              component="section"
              key={group.key}
              aria-labelledby={`group-${group.key}`}
              sx={{ minWidth: 0 }}
            >
              <Typography
                id={`group-${group.key}`}
                variant="h5"
                component="h2"
                sx={{ fontFamily: displayFont, fontWeight: 650, mb: 2.5 }}
              >
                {t(`landing.groups.${group.key}`)}
              </Typography>
              <Box
                component="ul"
                sx={{
                  listStyle: 'none',
                  m: 0,
                  p: 0,
                  display: 'flex',
                  flexDirection: 'column',
                  gap: 2.25,
                }}
              >
                {group.features.map((feature) => (
                  <Box
                    component="li"
                    key={feature.key}
                    sx={{
                      pl: 2,
                      borderLeft: 3,
                      borderColor: feature.color
                        ? `${feature.color}.main`
                        : 'divider',
                    }}
                  >
                    <Typography
                      component="h3"
                      sx={{ fontWeight: 650, mb: 0.25 }}
                    >
                      {t(`landing.features.${feature.key}.title`)}
                    </Typography>
                    <Typography
                      variant="body2"
                      sx={{ color: 'text.secondary', maxWidth: '36rem' }}
                    >
                      {t(`landing.features.${feature.key}.text`)}
                    </Typography>
                  </Box>
                ))}
              </Box>
            </Box>
          ))}
        </Box>

        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
            mt: { xs: 8, md: 12 },
            pt: 3,
            borderTop: 1,
            borderColor: 'divider',
          }}
        >
          {t('app.openSource')}{' '}
          <Box
            component="a"
            href={SOURCE_URL}
            target="_blank"
            rel="noreferrer"
            sx={{ color: 'primary.main' }}
          >
            GitHub
          </Box>
        </Typography>
        <Box sx={{ mt: 1 }}>
          <LegalLinks />
        </Box>
      </Container>
    </Box>
  );
}
