import { Button, Typography } from '@mui/material';
import { useQuery } from '@tanstack/react-query';

import { getMyselfOptions } from '@/generated/@tanstack/react-query.gen';
import { baseUrl, localClient } from '@/utils/QueryClient';
import { useEffect } from 'react';
import useUserStore from '@/store/UserStore';

export default function Login() {
  const setName = useUserStore((state) => state.setName);
  const setMaxCars = useUserStore((state) => state.setMaxCars);

  const { isError, isLoading, data } = useQuery({
    ...getMyselfOptions({
      client: localClient,
    }),
    retry: false,
    refetchIntervalInBackground: true,
    refetchInterval: 5 * 60 * 1000,
  });

  if (isError) {
    window.location.href = baseUrl + '/api/auth/login';
  }

  useEffect(() => {
    if (data?.name) {
      setName(data.name);
    }
    if (data?.maxCars) {
      setMaxCars(data.maxCars);
    }
  }, [data, setMaxCars, setName]);

  if (isLoading) {
    return (
      <Button variant="contained" href={baseUrl + '/api/auth/login'}>
        Login
      </Button>
    );
  }
  return (
    <Typography variant="body1">
      Logged in as <b>{data?.name}</b>
    </Typography>
  );
}
