// routes/LoginRoute.tsx
import { Alert, Box, Button, TextField, Typography } from '@mui/material';
import * as React from 'react';
import { useEffect, useState } from 'react';
import { createFileRoute, useNavigate } from '@tanstack/react-router';
import { useLoginMutation } from '@/wrapper/LoginWrapper';
import useUserStore from '@/store/UserStore';

export const Route = createFileRoute('/login/')({
  component: LoginRoute,
});

export function LoginRoute() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');

  const setLoggedIn = useUserStore((state) => state.setLoggedIn);

  const mutate = useLoginMutation();

  useEffect(() => {
    if (!mutate.isPending && mutate.data) {
      console.log('Logging in!');
      setLoggedIn(true);
      navigate({ to: '/' });
    }
  }, [setLoggedIn, mutate.isPending, mutate.data, navigate]);

  return (
    <Box
      display="flex"
      flexDirection="column"
      maxWidth={400}
      margin="auto"
      mt={10}
      p={4}
      boxShadow={3}
      borderRadius={2}
    >
      <Typography variant="h5" mb={2}>
        Login
      </Typography>

      {mutate.isError && (
        <Alert severity="error">Invalid credentials. Try again.</Alert>
      )}

      <TextField
        label="Email"
        type="email"
        fullWidth
        margin="normal"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      <TextField
        label="Password"
        type="password"
        fullWidth
        margin="normal"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <Button
        variant="contained"
        color="primary"
        fullWidth
        onClick={() => mutate.mutate({ password, username: email })}
        disabled={mutate.isPending}
        sx={{ mt: 2 }}
      >
        {mutate.isPending ? 'Logging in...' : 'Login'}
      </Button>
    </Box>
  );
}
