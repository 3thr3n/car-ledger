import {
  AppBar,
  Box,
  Container,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import productLogo from '@/assets/car-ledger.png';
import { useNavigate } from '@tanstack/react-router';
import { useState } from 'react';
import UserNavigation from '@/components/base/UserNavigation';
import { useTranslation } from 'react-i18next';

export default function Navigation() {
  const navi = useNavigate();
  const { t } = useTranslation();

  const goHome = async () => {
    await navi({
      to: '/',
    });
  };

  const theme = useTheme();
  const isSm = useMediaQuery(theme.breakpoints.down('md'));

  const [drawerOpen, setDrawerOpen] = useState(false);

  function toggleDrawer(open: boolean) {
    setDrawerOpen(open);
  }

  const navigate = async (path: string) => {
    await navi({
      to: path,
    });
    setDrawerOpen(false);
  };

  return (
    <AppBar
      position="relative"
      sx={{
        background: (theme) =>
          `linear-gradient(90deg, ${theme.palette.primary.light}22, transparent)`,
      }}
    >
      <Container maxWidth="xl">
        <Toolbar disableGutters sx={{ gap: 1 }}>
          <Box
            component="img"
            src={productLogo}
            alt="product_logo"
            sx={{
              height: isSm ? 40 : 64,
              width: 'auto',
              objectFit: 'contain',
            }}
          />
          <Box ml={1}>
            <Box
              onClick={goHome}
              sx={{
                cursor: 'pointer',
              }}
            >
              <Typography variant={isSm ? 'h6' : 'h4'} fontWeight={700}>
                {t('app.title')}
              </Typography>
            </Box>
          </Box>
          <UserNavigation
            isSm={isSm}
            toggleDrawer={toggleDrawer}
            drawerOpen={drawerOpen}
            navigate={navigate}
          />
        </Toolbar>
      </Container>
    </AppBar>
  );
}
