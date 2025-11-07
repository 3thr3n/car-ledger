import { useMemo } from 'react';
import { Box, Button, Paper, Typography } from '@mui/material';
import DirectionsCarIcon from '@mui/icons-material/DirectionsCar';
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation';
import AltRouteIcon from '@mui/icons-material/AltRoute';
import BuildCircleIcon from '@mui/icons-material/BuildCircle';
import TrafficIcon from '@mui/icons-material/Traffic';
import LocalCarWashIcon from '@mui/icons-material/LocalCarWash';
import EvStationIcon from '@mui/icons-material/EvStation';
import EngineeringIcon from '@mui/icons-material/Engineering';

const messages = [
  {
    title: 'Lost on the road?',
    description:
      'This page doesn’t seem to exist — maybe it took a wrong turn.',
    icon: <DirectionsCarIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Go back home',
  },
  {
    title: '404 — Flat tire!',
    description: 'Looks like this page didn’t make it to the destination.',
    icon: <LocalGasStationIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Back to the garage',
  },
  {
    title: 'Missed the turn!',
    description: 'Looks like you took the wrong exit.',
    icon: <AltRouteIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Back on track',
  },
  {
    title: 'Engine not found!',
    description: 'Something went missing under the hood.',
    icon: <BuildCircleIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Pop back to the garage',
  },
  {
    title: 'Red light ahead',
    description: 'Stop right there — this page doesn’t exist.',
    icon: <TrafficIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Go back safely',
  },
  {
    title: 'Car wash timeout',
    description: 'The page took too long to clean up.',
    icon: <LocalCarWashIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Return to the homepage',
  },
  {
    title: 'Out of fuel',
    description: 'This page ran out of gas.',
    icon: <EvStationIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Refuel at the homepage',
  },
  {
    title: 'Maintenance required',
    description: 'The page you’re looking for needs a tune-up.',
    icon: <EngineeringIcon sx={{ fontSize: 60, color: 'primary.main' }} />,
    button: 'Head back home',
  },
];

export default function NotFoundPage() {
  // Pick one message randomly on first render
  const message = useMemo(
    () => messages[Math.floor(Math.random() * messages.length)],
    [],
  );

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100%',
        backgroundColor: (theme) => theme.palette.background.default,
        px: 2,
      }}
    >
      <Paper
        elevation={3}
        sx={{
          p: 4,
          textAlign: 'center',
          maxWidth: 400,
          borderRadius: 4,
        }}
      >
        <Box sx={{ mb: 2 }}>{message.icon}</Box>
        <Typography variant="h5" fontWeight="bold" gutterBottom>
          {message.title}
        </Typography>
        <Typography variant="body1" sx={{ mb: 3 }}>
          {message.description}
        </Typography>
        <Button
          variant="contained"
          color="primary"
          onClick={() => (window.location.href = '/')}
        >
          {message.button}
        </Button>
      </Paper>
    </Box>
  );
}
