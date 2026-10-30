import {
  Box,
  Button,
  Chip,
  Divider,
  IconButton,
  List,
  ListItemIcon,
  ListItemText,
  ListSubheader,
  Stack,
  ToggleButton,
  ToggleButtonGroup,
  Tooltip,
  Typography,
  useColorScheme,
} from '@mui/material';
import SpaceDashboardRounded from '@mui/icons-material/SpaceDashboardRounded';
import GarageRounded from '@mui/icons-material/GarageRounded';
import NotificationsRounded from '@mui/icons-material/NotificationsRounded';
import AddRounded from '@mui/icons-material/AddRounded';
import LogoutRounded from '@mui/icons-material/LogoutRounded';
import SettingsRounded from '@mui/icons-material/SettingsRounded';
import AdminPanelSettingsRounded from '@mui/icons-material/AdminPanelSettingsRounded';
import LightModeRounded from '@mui/icons-material/LightModeRounded';
import DarkModeRounded from '@mui/icons-material/DarkModeRounded';
import SettingsBrightnessRounded from '@mui/icons-material/SettingsBrightnessRounded';
import MenuOpenRounded from '@mui/icons-material/MenuOpenRounded';
import PushPinOutlined from '@mui/icons-material/PushPinOutlined';
import { useQuery } from '@tanstack/react-query';
import { useLocation } from '@tanstack/react-router';
import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import useUnreadNotifications from '@/hooks/useUnreadNotifications';
import { isShared, ownCars } from '@/utils/carRole';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { baseUrl, localClient } from '@/utils/QueryClient';
import useMe from '@/hooks/useMe';
import useSaveSettings from '@/hooks/useSaveSettings';
import useOpenCarRequests from '@/hooks/useOpenCarRequests';
import useVersion from '@/hooks/useVersion';
import { reloadToNewest } from '@/utils/serviceWorker';
import { RouterListItemButton } from '@/components/ui/links';
import CarAvatar from '@/components/layout/CarAvatar';
import { offlineStore } from '@/offline';
import { forgetQueries } from '@/offline/persist';
import productLogo from '@/assets/car-ledger.svg';

const languages = ['EN', 'DE'];

/**
 * @param onHide hides the sidebar beside the pages, on a large screen
 * @param onPin shows it beside the pages again, from the menu
 */
export default function Sidebar({
  onNavigate,
  onHide,
  onPin,
}: {
  onNavigate?: () => void;
  onHide?: () => void;
  onPin?: () => void;
}) {
  const { t, i18n } = useTranslation();
  const { pathname } = useLocation();
  const { account, loggedIn, maxCars, isAdmin } = useMe();
  const settings = useSaveSettings();
  const openRequests = useOpenCarRequests();
  const unread = useUnreadNotifications();
  const version = useVersion();

  // the language of the settings wins over the one of the browser
  useEffect(() => {
    if (account?.language && account.language !== i18n.language) {
      i18n.changeLanguage(account.language);
    }
    // only when the saved language changes, not when the user switches
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [account?.language]);

  const changeLanguage = (language: string) => {
    i18n.changeLanguage(language);
    if (account) {
      settings.save({
        vatCountry: account.vatCountry,
        currency: account.currency,
        language,
        unitSystem: account.unitSystem,
      });
    }
  };
  const { mode, setMode } = useColorScheme();

  const { data: cars } = useQuery({
    ...getMyCarsOptions({ client: localClient }),
    enabled: loggedIn,
  });

  const canAddCar = ownCars(cars).length < maxCars;

  const nextMode =
    mode === 'light' ? 'dark' : mode === 'dark' ? 'system' : 'light';
  const modeIcon =
    mode === 'light' ? (
      <LightModeRounded fontSize="small" />
    ) : mode === 'dark' ? (
      <DarkModeRounded fontSize="small" />
    ) : (
      <SettingsBrightnessRounded fontSize="small" />
    );

  return (
    <Stack sx={{ height: '100%' }}>
      <Stack
        direction="row"
        spacing={1.25}
        sx={{
          alignItems: 'center',
          px: 2.5,
          py: 2.25,
        }}
      >
        <Box component="img" src={productLogo} alt="" sx={{ height: 30 }} />
        <Typography
          variant="h6"
          component="div"
          sx={{ fontWeight: 750, letterSpacing: '-0.02em' }}
        >
          {t('app.title')}
        </Typography>
        <Box sx={{ flexGrow: 1 }} />
        {onHide && (
          <Tooltip title={t('app.hideSidebar')}>
            <IconButton
              size="small"
              onClick={onHide}
              aria-label={t('app.hideSidebar')}
              sx={{ mr: -1 }}
            >
              <MenuOpenRounded fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
        {onPin && (
          <Tooltip title={t('app.pinSidebar')}>
            <IconButton
              size="small"
              onClick={onPin}
              aria-label={t('app.pinSidebar')}
              sx={{ mr: -1 }}
            >
              <PushPinOutlined fontSize="small" />
            </IconButton>
          </Tooltip>
        )}
      </Stack>

      {/* links, not list items: a list may only hold <li> */}
      <Box
        component="nav"
        aria-label={t('nav.label')}
        sx={{ flexGrow: 1, overflowY: 'auto', px: 1.5 }}
      >
        {loggedIn && (
          <>
            <List component="div" dense disablePadding>
              <RouterListItemButton
                to="/dashboard"
                selected={pathname.startsWith('/dashboard')}
                onClick={onNavigate}
                sx={{ borderRadius: 2, mb: 0.5 }}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <SpaceDashboardRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t('nav.dashboard')} />
              </RouterListItemButton>
              <RouterListItemButton
                to="/car"
                selected={pathname === '/car' || pathname === '/car/'}
                onClick={onNavigate}
                sx={{ borderRadius: 2 }}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <GarageRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t('nav.garage')} />
              </RouterListItemButton>
              <RouterListItemButton
                to="/notifications"
                selected={pathname.startsWith('/notifications')}
                onClick={onNavigate}
                sx={{ borderRadius: 2, mt: 0.5 }}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <NotificationsRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t('nav.notifications')} />
                {unread > 0 && (
                  <Tooltip title={t('inbox.unreadCount', { count: unread })}>
                    <Chip
                      size="small"
                      color="primary"
                      label={unread}
                      aria-label={t('inbox.unreadCount', { count: unread })}
                      sx={{ height: 20, fontWeight: 600 }}
                    />
                  </Tooltip>
                )}
              </RouterListItemButton>
              <RouterListItemButton
                to="/settings"
                selected={pathname.startsWith('/settings')}
                onClick={onNavigate}
                sx={{ borderRadius: 2, mt: 0.5 }}
              >
                <ListItemIcon sx={{ minWidth: 36 }}>
                  <SettingsRounded fontSize="small" />
                </ListItemIcon>
                <ListItemText primary={t('nav.settings')} />
              </RouterListItemButton>
              {isAdmin && (
                <RouterListItemButton
                  to="/admin"
                  selected={pathname.startsWith('/admin')}
                  onClick={onNavigate}
                  sx={{ borderRadius: 2, mt: 0.5 }}
                >
                  <ListItemIcon sx={{ minWidth: 36 }}>
                    <AdminPanelSettingsRounded fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary={t('nav.admin')} />
                  {openRequests > 0 && (
                    <Tooltip
                      title={t('admin.requests.openCount', {
                        count: openRequests,
                      })}
                    >
                      <Chip
                        size="small"
                        color="error"
                        label={openRequests}
                        aria-label={t('admin.requests.openCount', {
                          count: openRequests,
                        })}
                        sx={{ height: 20, fontWeight: 600 }}
                      />
                    </Tooltip>
                  )}
                </RouterListItemButton>
              )}
            </List>

            <List
              component="div"
              dense
              disablePadding
              sx={{ mt: 2 }}
              subheader={
                <ListSubheader
                  component="div"
                  disableSticky
                  sx={{
                    bgcolor: 'transparent',
                    lineHeight: '32px',
                    fontWeight: 600,
                    px: 1,
                  }}
                >
                  {t('nav.yourCars')}
                </ListSubheader>
              }
            >
              {cars?.map((car) => (
                <RouterListItemButton
                  key={car.id}
                  to="/car/$id"
                  params={{ id: `${car.id}` }}
                  selected={pathname.startsWith(`/car/${car.id}`)}
                  onClick={onNavigate}
                  sx={{ borderRadius: 2, mb: 0.25, py: 0.75 }}
                >
                  <ListItemIcon sx={{ minWidth: 42 }}>
                    <CarAvatar
                      id={car.id}
                      name={car.name}
                      pictureId={car.pictureId}
                      size={28}
                    />
                  </ListItemIcon>
                  <ListItemText
                    primary={car.name}
                    secondary={
                      isShared(car)
                        ? t('car.shared', { role: t(`car.roles.${car.role}`) })
                        : t('garage.entries', { count: car.amountBills ?? 0 })
                    }
                    slotProps={{
                      primary: { noWrap: true, sx: { fontWeight: 500 } },
                      secondary: { noWrap: true, variant: 'caption' },
                    }}
                  />
                </RouterListItemButton>
              ))}
              {canAddCar && (
                <RouterListItemButton
                  to="/car/new"
                  onClick={onNavigate}
                  sx={{ borderRadius: 2, color: 'text.secondary' }}
                >
                  <ListItemIcon sx={{ minWidth: 42, pl: 0.5 }}>
                    <AddRounded fontSize="small" />
                  </ListItemIcon>
                  <ListItemText primary={t('garage.add')} />
                </RouterListItemButton>
              )}
            </List>
          </>
        )}
      </Box>

      <Divider />
      <Stack spacing={1.5} sx={{ p: 2 }}>
        <Stack
          direction="row"
          spacing={1}
          sx={{
            alignItems: 'center',
          }}
        >
          <ToggleButtonGroup
            size="small"
            exclusive
            value={i18n.language}
            onChange={(_, value) => value && changeLanguage(value)}
            aria-label={t('app.language')}
          >
            {languages.map((lang) => (
              <ToggleButton key={lang} value={lang} sx={{ px: 1.25, py: 0.25 }}>
                {lang}
              </ToggleButton>
            ))}
          </ToggleButtonGroup>
          <Box sx={{ flexGrow: 1 }} />
          <Tooltip title={t(`theme.${mode ?? 'system'}`)}>
            <IconButton size="small" onClick={() => setMode(nextMode)}>
              {modeIcon}
            </IconButton>
          </Tooltip>
        </Stack>

        {loggedIn ? (
          <Stack
            direction="row"
            spacing={1}
            sx={{
              alignItems: 'center',
            }}
          >
            <Box sx={{ minWidth: 0, flexGrow: 1 }}>
              <Typography
                variant="caption"
                sx={{
                  color: 'text.secondary',
                }}
              >
                {t('nav.signedInAs')}
              </Typography>
              <Typography
                variant="body2"
                noWrap
                sx={{
                  fontWeight: 600,
                }}
              >
                {account?.name}
              </Typography>
            </Box>
            <Tooltip title={t('button.logout')}>
              <IconButton
                size="small"
                href={`${baseUrl}/api/auth/logout`}
                // the next user of the device doesn't see the kept data: it is
                // deleted before the browser leaves the page
                onClick={(event) => {
                  event.preventDefault();
                  const logout = event.currentTarget.href;
                  void forgetQueries(offlineStore).finally(() =>
                    location.assign(logout),
                  );
                }}
              >
                <LogoutRounded fontSize="small" />
              </IconButton>
            </Tooltip>
          </Stack>
        ) : (
          <Button
            variant="contained"
            href={`${baseUrl}/api/auth/login`}
            fullWidth
          >
            {t('button.login')}
          </Button>
        )}
        {loggedIn && (
          <Stack
            direction="row"
            spacing={1}
            sx={{ alignItems: 'center', justifyContent: 'space-between' }}
            data-testid="app-version"
          >
            <Typography variant="caption" sx={{ color: 'text.secondary' }}>
              {t('app.version', { version: version.app })}
            </Typography>
            {version.outdated && (
              <Button
                size="small"
                color="warning"
                onClick={reloadToNewest}
                sx={{ py: 0, minWidth: 0 }}
              >
                {t('app.update.available')}
              </Button>
            )}
          </Stack>
        )}
      </Stack>
    </Stack>
  );
}
