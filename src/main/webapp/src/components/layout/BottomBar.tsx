import { Badge, Box, ButtonBase, Theme, Typography } from '@mui/material';
import SpaceDashboardRounded from '@mui/icons-material/SpaceDashboardRounded';
import GarageRounded from '@mui/icons-material/GarageRounded';
import LocalGasStationRounded from '@mui/icons-material/LocalGasStationRounded';
import EvStationRounded from '@mui/icons-material/EvStationRounded';
import NotificationsRounded from '@mui/icons-material/NotificationsRounded';
import MenuRounded from '@mui/icons-material/MenuRounded';
import { useQuery } from '@tanstack/react-query';
import { createLink, useRouterState } from '@tanstack/react-router';
import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { BillType } from '@/generated';
import { getMyCarsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { shortcutCar, shortcutType } from '@/utils/lastCar';
import useUnreadNotifications from '@/hooks/useUnreadNotifications';
import useOpenCarRequests from '@/hooks/useOpenCarRequests';

/** the height of the bar, without the safe area of the phone below it */
export const BOTTOM_BAR_HEIGHT = 64;

// with a href the button base renders a link
const RouterButtonBase = createLink(ButtonBase);

const itemSx = (theme: Theme) =>
  ({
    flexDirection: 'column',
    gap: 0.25,
    minWidth: 0,
    height: BOTTOM_BAR_HEIGHT,
    color: 'text.secondary',
    borderRadius: 2,
    '&[aria-current="page"]': { color: 'primary.main' },
    '&.Mui-focusVisible': {
      outline: `2px solid ${theme.vars.palette.primary.main}`,
      outlineOffset: -4,
    },
  }) as const;

function Label({ children }: { children: ReactNode }) {
  return (
    <Typography
      component="span"
      noWrap
      sx={{ fontSize: 11, fontWeight: 600, lineHeight: 1.2, maxWidth: '100%' }}
    >
      {children}
    </Typography>
  );
}

/**
 * The main places on a phone, in reach of the thumb: the dashboard, the garage,
 * a new refuel (or charge) of the last opened car in the middle, the inbox and
 * the menu with the cars, the settings and the rest
 */
export default function BottomBar({ onMenu }: { onMenu: () => void }) {
  const { t } = useTranslation();
  const pathname = useRouterState({ select: (s) => s.location.pathname });
  const { data: cars } = useQuery(getMyCarsOptions({ client: localClient }));
  const unread = useUnreadNotifications();
  const openRequests = useOpenCarRequests();
  const car = cars && shortcutCar(cars);
  const type = car && shortcutType(car);
  const charging = type === BillType.CHARGING;

  return (
    <Box
      component="nav"
      aria-label={t('nav.shortcuts')}
      sx={(theme) => ({
        position: 'fixed',
        zIndex: theme.zIndex.appBar,
        left: 0,
        right: 0,
        bottom: 0,
        pb: 'env(safe-area-inset-bottom)',
        display: 'grid',
        gridTemplateColumns: 'repeat(5, minmax(0, 1fr))',
        alignItems: 'center',
        px: 0.5,
        bgcolor: 'background.paper',
        borderTop: 1,
        borderColor: 'divider',
        ...theme.applyStyles('dark', { bgcolor: 'background.default' }),
      })}
    >
      <RouterButtonBase
        to="/dashboard"
        aria-current={pathname.startsWith('/dashboard') ? 'page' : undefined}
        sx={itemSx}
      >
        <SpaceDashboardRounded />
        <Label>{t('nav.dashboard')}</Label>
      </RouterButtonBase>
      <RouterButtonBase
        to="/car"
        aria-current={pathname.startsWith('/car') ? 'page' : undefined}
        sx={itemSx}
      >
        <GarageRounded />
        <Label>{t('nav.garage')}</Label>
      </RouterButtonBase>

      {car?.id !== undefined && type ? (
        <RouterButtonBase
          to="/car/$id/bill/add"
          params={{ id: `${car.id}` }}
          search={{ type }}
          sx={(theme) => ({ ...itemSx(theme), color: 'text.primary' })}
        >
          <Box
            component="span"
            sx={(theme) => ({
              display: 'grid',
              placeItems: 'center',
              width: 52,
              height: 52,
              mt: -3.5,
              borderRadius: '50%',
              bgcolor: 'primary.main',
              color: 'primary.contrastText',
              // a ring of the ground, the button stands out of the bar
              boxShadow: `0 0 0 4px ${theme.vars.palette.background.paper}`,
              ...theme.applyStyles('dark', {
                boxShadow: `0 0 0 4px ${theme.vars.palette.background.default}`,
              }),
            })}
          >
            {charging ? <EvStationRounded /> : <LocalGasStationRounded />}
          </Box>
          <Label>{t(charging ? 'nav.charge' : 'nav.refuel')}</Label>
        </RouterButtonBase>
      ) : (
        // no car to refuel yet: the place stays, the others keep their places
        <span />
      )}

      <RouterButtonBase
        to="/notifications"
        aria-current={
          pathname.startsWith('/notifications') ? 'page' : undefined
        }
        sx={itemSx}
      >
        <Badge
          badgeContent={unread}
          color="primary"
          max={99}
          slotProps={{
            badge: unread
              ? { 'aria-label': t('inbox.unreadCount', { count: unread }) }
              : {},
          }}
        >
          <NotificationsRounded />
        </Badge>
        <Label>{t('nav.inbox')}</Label>
      </RouterButtonBase>
      <ButtonBase onClick={onMenu} sx={itemSx}>
        <Badge
          variant="dot"
          color="error"
          invisible={openRequests === 0}
          slotProps={{
            badge: openRequests
              ? {
                  'aria-label': t('admin.requests.openCount', {
                    count: openRequests,
                  }),
                }
              : {},
          }}
        >
          <MenuRounded />
        </Badge>
        <Label>{t('app.menu')}</Label>
      </ButtonBase>
    </Box>
  );
}
