import {
  AppBar,
  Box,
  Container,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import queryClient from '@/utils/QueryClient';
import productLogo from '@/assets/car-ledger.png';
import { useNavigate } from '@tanstack/react-router';
import { useState } from 'react';
import UserNavigation from '@/components/base/UserNavigation';

export default function Navigation() {
  const navi = useNavigate();

  const goHome = async () => {
    await navi({
      to: '/',
    });
  };

  const theme = useTheme();
  const isSm = useMediaQuery(theme.breakpoints.down('sm'));

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
    <QueryClientProvider client={queryClient}>
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
                  CarLedger
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
    </QueryClientProvider>
  );
}
