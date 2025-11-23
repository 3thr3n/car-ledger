import {
  Box,
  Divider,
  Drawer,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import Login from '@/components/base/Login';
import useUserStore from '@/store/UserStore';
import { LanguageSwitcher } from '@/components/base/LanguageSwitcher';
import { useLocation } from '@tanstack/react-router';
import { useTranslation } from 'react-i18next';
import TopNav from '@/components/base/nav/TopNav';
import TopDrawerNav from '@/components/base/nav/TopDrawerNav';

export interface UserNavigationProps {
  isSm: boolean;
  toggleDrawer: (open: boolean) => void;
  drawerOpen: boolean;
  navigate: (path: string) => void;
}

const navItems = [
  { labelKey: 'app.nav.dashboard', path: '/dashboard' },
  { labelKey: 'app.nav.cars', path: '/car' },
];

export default function UserNavigation({
  isSm,
  toggleDrawer,
  drawerOpen,
  navigate,
}: UserNavigationProps) {
  const isLoggedIn = useUserStore((state) => state.loggedIn);
  const location = useLocation();
  const { t } = useTranslation();

  if (isSm) {
    return (
      <>
        <Box
          sx={{
            flexGrow: 1,
          }}
        />
        <IconButton color="inherit" onClick={() => toggleDrawer(true)}>
          <MenuIcon />
        </IconButton>

        <Drawer
          anchor="right"
          open={drawerOpen}
          onClose={() => toggleDrawer(false)}
        >
          <Box
            sx={{
              width: 250,
              p: 2,
              height: '100%',
            }}
            role="presentation"
          >
            <Stack spacing={1} direction="column" sx={{ height: '100%' }}>
              <Typography variant="h6" gutterBottom>
                {t('app.nav.title')}
              </Typography>
              <Divider sx={{ mb: 2 }} />
              <Login drawerMode />
              <Divider sx={{ mb: 2 }} />
              {/* Navigation List */}
              {isLoggedIn && (
                <TopDrawerNav
                  t={t}
                  navigate={navigate}
                  currentPathName={location.pathname}
                  navItems={navItems}
                />
              )}
              <Box sx={{ flexGrow: 1 }} />
              <LanguageSwitcher drawerMode />
            </Stack>
          </Box>
        </Drawer>
      </>
    );
  }

  return (
    <>
      <Box
        sx={{
          marginY: '1rem',
        }}
      />
      {isLoggedIn && (
        <TopNav
          t={t}
          navigate={navigate}
          currentPathName={location.pathname}
          navItems={navItems}
        />
      )}
      <Box
        sx={{
          flexGrow: 1,
        }}
      />
      <Login />
      <LanguageSwitcher />
    </>
  );
}
