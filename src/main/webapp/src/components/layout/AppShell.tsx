import {
  Alert,
  AppBar,
  Badge,
  Box,
  Drawer,
  IconButton,
  LinearProgress,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuRounded from '@mui/icons-material/MenuRounded';
import CloudOffRounded from '@mui/icons-material/CloudOffRounded';
import { ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';
import useOpenInvitations from '@/hooks/useOpenInvitations';
import Sidebar from '@/components/layout/Sidebar';
import useMe from '@/hooks/useMe';
import useOnline from '@/hooks/useOnline';
import useOpenCarRequests from '@/hooks/useOpenCarRequests';
import productLogo from '@/assets/car-ledger.svg';
import OfflineSync from '@/offline/OfflineSync';

const SIDEBAR_WIDTH = 264;

export default function AppShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [open, setOpen] = useState(false);
  // the pages start with the settings of the account, e.g. a form with its
  // units: rendered once the account is loaded, or known to be missing. Not
  // when it is fetched again, a query with an error is fetched again whenever
  // a page mounts it.
  const { isFetched } = useMe();
  // the sidebar is behind the menu on phones: a dot shows the open requests
  const openRequests = useOpenCarRequests();
  const invitations = useOpenInvitations().length;
  const online = useOnline();

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100dvh',
        bgcolor: 'background.default',
      }}
    >
      <OfflineSync />
      {isDesktop ? (
        <Box
          component="nav"
          sx={{
            width: SIDEBAR_WIDTH,
            flexShrink: 0,
            position: 'sticky',
            top: 0,
            height: '100dvh',
            borderRight: 1,
            borderColor: 'divider',
            bgcolor: 'background.paper',
          }}
        >
          <Sidebar />
        </Box>
      ) : (
        <>
          <AppBar
            position="fixed"
            color="inherit"
            sx={{
              borderBottom: 1,
              borderColor: 'divider',
              bgcolor: 'background.paper',
            }}
          >
            <Toolbar sx={{ gap: 1 }}>
              <IconButton
                edge="start"
                onClick={() => setOpen(true)}
                aria-label="menu"
              >
                <Badge
                  variant="dot"
                  color="error"
                  invisible={openRequests === 0 && invitations === 0}
                  slotProps={{
                    badge: {
                      'aria-label': [
                        openRequests > 0 &&
                          t('admin.requests.openCount', {
                            count: openRequests,
                          }),
                        invitations > 0 &&
                          t('access.openCount', { count: invitations }),
                      ]
                        .filter(Boolean)
                        .join(', '),
                    },
                  }}
                >
                  <MenuRounded />
                </Badge>
              </IconButton>
              <Box
                component="img"
                src={productLogo}
                alt=""
                sx={{ height: 26 }}
              />
              <Typography variant="h6" sx={{ fontWeight: 750 }}>
                {t('app.title')}
              </Typography>
            </Toolbar>
          </AppBar>
          <Drawer
            open={open}
            onClose={() => setOpen(false)}
            slotProps={{ paper: { sx: { width: SIDEBAR_WIDTH } } }}
          >
            <Sidebar onNavigate={() => setOpen(false)} />
          </Drawer>
        </>
      )}

      <Box
        component="main"
        sx={{
          flexGrow: 1,
          minWidth: 0,
          pt: isDesktop ? 0 : 7,
          display: 'flex',
          flexDirection: 'column',
        }}
      >
        {!online && (
          <Alert
            severity="warning"
            icon={<CloudOffRounded fontSize="inherit" />}
            data-testid="offline-banner"
            sx={{ borderRadius: 0 }}
          >
            {t('offline.banner')}
          </Alert>
        )}
        {!isFetched ? (
          <LinearProgress aria-label={t('app.loading')} />
        ) : (
          children
        )}
      </Box>
    </Box>
  );
}
