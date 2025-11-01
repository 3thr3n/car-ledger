import {
  AppBar,
  Box,
  Container,
  Divider,
  Drawer,
  IconButton,
  Toolbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import Login from './Login';
import queryClient from '@/utils/QueryClient';
import productLogo from '@/assets/car-ledger.png';
import { useNavigate } from '@tanstack/react-router';
import { useState } from 'react';
import MenuIcon from '@mui/icons-material/Menu';

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

  const toggleDrawer = (open: boolean) => () => {
    setDrawerOpen(open);
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
            <Box
              sx={{
                flexGrow: 1,
              }}
            />
            {isSm ? (
              <>
                <IconButton color="inherit" onClick={toggleDrawer(true)}>
                  <MenuIcon />
                </IconButton>

                <Drawer
                  anchor="right"
                  open={drawerOpen}
                  onClose={toggleDrawer(false)}
                >
                  <Box
                    sx={{
                      width: 250,
                      display: 'flex',
                      flexDirection: 'column',
                      p: 2,
                      height: '100%',
                    }}
                    role="presentation"
                  >
                    <Typography variant="h6" gutterBottom>
                      Menu
                    </Typography>
                    <Divider sx={{ mb: 2 }} />
                    <Login drawerMode />
                  </Box>
                </Drawer>
              </>
            ) : (
              <Box>
                <Login />
              </Box>
            )}
          </Toolbar>
        </Container>
      </AppBar>
    </QueryClientProvider>
  );
}
