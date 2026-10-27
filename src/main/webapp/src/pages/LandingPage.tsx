import {
  Alert,
  Box,
  Button,
  Card,
  Container,
  Grid,
  Stack,
  Typography,
} from '@mui/material';
import ArrowForwardRounded from '@mui/icons-material/ArrowForwardRounded';
import UploadFileRounded from '@mui/icons-material/UploadFileRounded';
import InsightsRounded from '@mui/icons-material/InsightsRounded';
import TuneRounded from '@mui/icons-material/TuneRounded';
import FactCheckRounded from '@mui/icons-material/FactCheckRounded';
import PictureAsPdfRounded from '@mui/icons-material/PictureAsPdfRounded';
import NotificationsActiveRounded from '@mui/icons-material/NotificationsActiveRounded';
import RouteRounded from '@mui/icons-material/RouteRounded';
import GpsFixedRounded from '@mui/icons-material/GpsFixedRounded';
import InstallMobileRounded from '@mui/icons-material/InstallMobileRounded';
import BackupRounded from '@mui/icons-material/BackupRounded';
import GroupRounded from '@mui/icons-material/GroupRounded';
import { ReactNode, useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import useMe from '@/hooks/useMe';
import {
  forgetAccountDeleted,
  wasAccountDeleted,
} from '@/utils/accountDeleted';
import { baseUrl } from '@/utils/QueryClient';
import { RouterButton } from '@/components/ui/links';
import { billTypes, BillPaletteKey } from '@/utils/billTypes';
import { BillType } from '@/generated';
import { SOURCE_URL } from '@/utils/source';

interface Feature {
  key: string;
  icon: ReactNode;
  color: BillPaletteKey | 'primary';
}

/** the costs first, then the papers of the car, the road and the data */
const features: Feature[] = [
  { key: 'fuel', icon: billTypes[BillType.FUEL].icon, color: 'fuel' },
  {
    key: 'charging',
    icon: billTypes[BillType.CHARGING].icon,
    color: 'charging',
  },
  {
    key: 'maintenance',
    icon: billTypes[BillType.MAINTENANCE].icon,
    color: 'maintenance',
  },
  {
    key: 'recurring',
    icon: billTypes[BillType.RECURRING].icon,
    color: 'recurring',
  },
  {
    key: 'miscellaneous',
    icon: billTypes[BillType.MISCELLANEOUS].icon,
    color: 'miscellaneous',
  },
  {
    key: 'stats',
    icon: <InsightsRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'modifications',
    icon: <TuneRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'inspections',
    icon: <FactCheckRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'papers',
    icon: <PictureAsPdfRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'reminders',
    icon: <NotificationsActiveRounded fontSize="small" />,
    color: 'primary',
  },
  { key: 'trips', icon: <RouteRounded fontSize="small" />, color: 'primary' },
  { key: 'gps', icon: <GpsFixedRounded fontSize="small" />, color: 'primary' },
  {
    key: 'sharing',
    icon: <GroupRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'offline',
    icon: <InstallMobileRounded fontSize="small" />,
    color: 'primary',
  },
  {
    key: 'import',
    icon: <UploadFileRounded fontSize="small" />,
    color: 'primary',
  },
  { key: 'backup', icon: <BackupRounded fontSize="small" />, color: 'primary' },
];

export default function LandingPage() {
  const { t } = useTranslation();
  const { loggedIn } = useMe();

  // read once, a toast would fire before the toast container is ready
  const [accountDeleted] = useState(wasAccountDeleted);
  useEffect(forgetAccountDeleted, []);

  return (
    <Box id="LandingPage" sx={{ py: { xs: 5, md: 10 } }}>
      <Container maxWidth="md">
        {accountDeleted && !loggedIn && (
          <Alert severity="success" sx={{ mb: 4 }}>
            {t('settings.deleteAccount.done')}
          </Alert>
        )}
        <Stack
          spacing={3}
          sx={{
            alignItems: 'flex-start',
            mb: { xs: 6, md: 9 },
          }}
        >
          <Typography variant="overline" color="primary">
            {t('landing.eyebrow')}
          </Typography>
          <Typography
            variant="h2"
            component="h1"
            sx={{
              fontSize: { xs: '2.25rem', md: '3.25rem' },
              textWrap: 'balance',
            }}
          >
            {t('landing.title')}
          </Typography>
          <Typography
            variant="h6"
            component="p"
            sx={{
              color: 'text.secondary',
              fontWeight: 400,
              maxWidth: 560,
            }}
          >
            {t('landing.subtitle')}
          </Typography>
          {loggedIn ? (
            <RouterButton
              to="/dashboard"
              variant="contained"
              size="large"
              endIcon={<ArrowForwardRounded />}
            >
              {t('landing.ctaDashboard')}
            </RouterButton>
          ) : (
            <Button
              href={`${baseUrl}/api/auth/login`}
              variant="contained"
              size="large"
              endIcon={<ArrowForwardRounded />}
            >
              {t('landing.cta')}
            </Button>
          )}
        </Stack>

        <Grid container spacing={2}>
          {features.map((feature) => (
            <Grid key={feature.key} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card sx={{ p: 2.5, height: '100%' }}>
                <Box
                  sx={(theme) => ({
                    display: 'grid',
                    placeItems: 'center',
                    width: 36,
                    height: 36,
                    borderRadius: 2.5,
                    mb: 1.5,
                    color: theme.vars.palette[feature.color].main,
                    backgroundColor: `rgba(${theme.vars.palette[feature.color].mainChannel} / 0.12)`,
                  })}
                >
                  {feature.icon}
                </Box>
                <Typography
                  variant="subtitle1"
                  sx={{
                    fontWeight: 650,
                  }}
                >
                  {t(`landing.features.${feature.key}.title`)}
                </Typography>
                <Typography
                  variant="body2"
                  sx={{
                    color: 'text.secondary',
                  }}
                >
                  {t(`landing.features.${feature.key}.text`)}
                </Typography>
              </Card>
            </Grid>
          ))}
        </Grid>

        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
            mt: 6,
            textAlign: 'center',
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
      </Container>
    </Box>
  );
}
