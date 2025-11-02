import { Box, Button, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import {
  getMyselfOptions,
  logoutOptions,
} from '@/generated/@tanstack/react-query.gen';
import { baseUrl, localClient } from '@/utils/QueryClient';
import { useEffect } from 'react';
import useUserStore from '@/store/UserStore';

interface LoginProps {
  drawerMode?: boolean;
}

export default function Login({ drawerMode = false }: LoginProps) {
  const isLoggedIn = useUserStore((state) => state.loggedIn);
  const setName = useUserStore((state) => state.setName);
  const setLoggedIn = useUserStore((state) => state.setLoggedIn);
  const setMaxCars = useUserStore((state) => state.setMaxCars);

  const logoutQuery = useQuery({
    ...logoutOptions({
      client: localClient,
    }),
    enabled: false,
    retry: false,
  });

  const { isError, isLoading, data, refetch } = useQuery({
    ...getMyselfOptions({
      client: localClient,
    }),
    retry: false,
    refetchIntervalInBackground: true,
    refetchInterval: 5 * 60 * 1000,
  });

  async function logout() {
    await logoutQuery.refetch();
    await refetch();
    setLoggedIn(false);
  }

  if (isError) {
    setLoggedIn(false);
  }

  useEffect(() => {
    if (data) {
      if (data.name) {
        setName(data.name);
      }
      if (data.maxCars) {
        setMaxCars(data.maxCars);
      }
      setLoggedIn(true);
    }
  }, [data, setMaxCars, setName, setLoggedIn]);

  if (isLoading || !isLoggedIn) {
    return (
      <Button
        variant="contained"
        href={baseUrl + '/api/auth/login'}
        fullWidth={drawerMode} // full width in drawer
        sx={{ mb: drawerMode ? 2 : 0 }}
      >
        Login
      </Button>
    );
  }
  return (
    <Box
      display="flex"
      flexDirection={drawerMode ? 'column' : 'row'}
      alignItems={drawerMode ? 'flex-start' : 'center'}
      gap={drawerMode ? 1 : 2}
    >
      <Typography variant="body1">
        Welcome <b>{data?.name}</b>
      </Typography>
      <Button variant="contained" onClick={logout} fullWidth={drawerMode}>
        Logout
      </Button>
    </Box>
  );
}
