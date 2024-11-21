import { Button, Typography } from "@mui/material";
import { useQuery } from "@tanstack/react-query";

import { getMyselfOptions } from '@/generated/@tanstack/react-query.gen'
import { localClient, baseUrl } from "@/utils/QueryClient";

export default function Login() {
  const { isError, isLoading } = useQuery({
    ...getMyselfOptions({
      client: localClient
    }),
    retry: false,
    refetchIntervalInBackground: true,
    refetchInterval: 5 * 60 * 1000
  })

  if (isError) {
    window.location.href=baseUrl + "/api/auth/login"
  }

  function renderComponent() {
    if (isLoading) {
      return <Button variant="contained" href={baseUrl + "/api/auth/login"}>Login</Button>
    }
    return <Typography>LoggedIn</Typography>
  }

  return renderComponent();
}