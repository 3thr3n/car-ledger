import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import MenuRounded from '@mui/icons-material/MenuRounded';
import { ReactNode, useState } from 'react';
import { useTranslation } from 'react-i18next';
import Sidebar from '@/components/layout/Sidebar';
import productLogo from '@/assets/car-ledger.svg';

const SIDEBAR_WIDTH = 264;

export default function AppShell({ children }: { children: ReactNode }) {
  const { t } = useTranslation();
  const theme = useTheme();
  const isDesktop = useMediaQuery(theme.breakpoints.up('md'));
  const [open, setOpen] = useState(false);

  return (
    <Box
      sx={{
        display: 'flex',
        minHeight: '100dvh',
        bgcolor: 'background.default',
      }}
    >
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
                <MenuRounded />
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
        {children}
      </Box>
    </Box>
  );
}
