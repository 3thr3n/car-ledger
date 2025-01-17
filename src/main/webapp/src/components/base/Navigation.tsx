import { AppBar, Box, Container, Toolbar, Typography } from '@mui/material';
import { QueryClientProvider } from '@tanstack/react-query';
import Login from './Login';
import queryClient from '@/utils/QueryClient';
import productLogo from '@/assets/car-ledger.png';
import { useNavigate } from '@tanstack/react-router';

export default function Navigation() {
  const navi = useNavigate();

  const goHome = async () => {
    await navi({
      to: '/',
    });
  };

  return (
    <QueryClientProvider client={queryClient}>
      <AppBar position="relative">
        <Container maxWidth="xl">
          <Toolbar>
            <img
              src={productLogo}
              alt="product_logo"
              style={{
                maxHeight: '64px',
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
                <Typography variant="h5">CarLedger</Typography>
              </Box>
            </Box>
            <Box
              sx={{
                flexGrow: 1,
              }}
            />
            <Box>
              <Login />
            </Box>
          </Toolbar>
        </Container>
      </AppBar>
    </QueryClientProvider>
  );
}
