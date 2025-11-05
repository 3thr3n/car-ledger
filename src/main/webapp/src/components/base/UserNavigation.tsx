import {
  Box,
  Button,
  Card,
  Divider,
  Drawer,
  IconButton,
  Stack,
  Typography,
} from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import Login from '@/components/base/Login';
import useUserStore from '@/store/UserStore';

export interface UserNavigationProps {
  isSm: boolean;
  toggleDrawer: (open: boolean) => void;
  drawerOpen: boolean;
  navigate: (path: string) => void;
}

export default function UserNavigation({
  isSm,
  toggleDrawer,
  drawerOpen = false,
  navigate,
}: UserNavigationProps) {
  const isLoggedIn = useUserStore((state) => state.loggedIn);

  function drawerNavigationButton(name: string, path: string) {
    return (
      <Card
        sx={{
          p: 1,
          textAlign: 'center',
          cursor: 'pointer',
          bgcolor: 'background.paper',
          color: 'text.primary',
          transition: '0.2s',
        }}
        onClick={() => navigate(path)}
      >
        <Typography variant="body2">{name}</Typography>
      </Card>
    );
  }

  function navigationButton(name: string, path: string) {
    return (
      <Button variant="contained" onClick={() => navigate(path)}>
        {name}
      </Button>
    );
  }

  if (isSm) {
    return (
      <>
        <Box
          sx={{
            flexGrow: 1,
          }}
        />
        <IconButton color="inherit" onClick={toggleDrawer(true)}>
          <MenuIcon />
        </IconButton>

        <Drawer anchor="right" open={drawerOpen} onClose={toggleDrawer(false)}>
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
            <Stack spacing={1}>
              <Login drawerMode />
              {isLoggedIn && drawerNavigationButton('Dashboard', '/dashboard')}
              {isLoggedIn && drawerNavigationButton('Cars', '/car')}
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
      {isLoggedIn && navigationButton('Cars', '/car')}
      {isLoggedIn && navigationButton('Dashboard', '/dashboard')}
      <Box
        sx={{
          flexGrow: 1,
        }}
      />
      <Login />
    </>
  );
}
