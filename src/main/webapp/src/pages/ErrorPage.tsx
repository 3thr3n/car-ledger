import React, { useEffect, useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Box, Button, Container, Typography } from '@mui/material';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';

type ErrorPageProps = {
  error?: unknown;
};

export default function ErrorPage({ error }: ErrorPageProps) {
  const navigate = useNavigate();
  const [countdown, setCountdown] = useState(10);

  const handleGoHome = async () => {
    await navigate({ to: '/' });
  };

  // Optionally, extract message from the error
  const message =
    error instanceof Error
      ? error.message
      : typeof error === 'string'
        ? error
        : 'An unexpected error occurred.';

  useEffect(() => {
    const timer = setInterval(() => {
      setCountdown((prev) => prev - 1);
    }, 1000);

    const reload = setTimeout(async () => {
      await handleGoHome(); // 🔁 auto reload after 10 seconds
    }, 10000);

    return () => {
      clearInterval(timer);
      clearTimeout(reload);
    };
  }, []);

  return (
    <Container
      maxWidth="sm"
      sx={{
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        alignItems: 'center',
        textAlign: 'center',
        gap: 3,
      }}
    >
      <ErrorOutlineIcon color="error" sx={{ fontSize: 80 }} />

      <Box>
        <Typography variant="h3" fontWeight={700} gutterBottom>
          Oops!
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ mb: 3 }}>
          {message}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Going to Homepage in <strong>{countdown}</strong> second
          {countdown !== 1 ? 's' : ''}...
        </Typography>
      </Box>

      <Button
        variant="contained"
        color="primary"
        onClick={handleGoHome}
        sx={{ px: 4, py: 1.5, borderRadius: 3 }}
      >
        Go Back Home
      </Button>
    </Container>
  );
}
