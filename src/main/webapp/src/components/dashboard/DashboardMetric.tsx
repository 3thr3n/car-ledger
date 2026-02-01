import { Box, Card, CardContent, Typography } from '@mui/material';
import { ReactNode } from 'react';
import EuroIcon from '@mui/icons-material/Euro';
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation';
import RouteIcon from '@mui/icons-material/Route';
import { QuestionMark } from '@mui/icons-material';

export default function DashboardMetric({
  label,
  value,
  unit,
  type,
}: {
  label: string;
  value: string | number;
  unit?: string;
  type: 'distance' | 'fuel' | 'cost' | 'div';
}) {
  const asdf: { icon?: ReactNode; color?: string } = {};

  if (type == 'distance') {
    asdf.icon = <RouteIcon />;
    asdf.color = '#4dd0e1';
  } else if (type == 'fuel') {
    asdf.icon = <LocalGasStationIcon />;
    asdf.color = '#ba68c8';
  } else if (type == 'cost') {
    asdf.icon = <EuroIcon />;
    asdf.color = '#ffb74d';
  } else {
    asdf.icon = <QuestionMark />;
    asdf.color = '#f2f2f2';
  }

  return (
    <Card
      sx={{
        p: 2,
        borderRadius: 4,
        background: '#1a1a1a',
        border: '1px solid #2a2a2a',
        minHeight: 120,
      }}
    >
      <CardContent
        sx={{
          display: 'flex',
          gap: 2,
          textAlign: 'center',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <Box
          sx={{
            width: 50,
            height: 50,
            borderRadius: 3,
            background: asdf.color + '20',
            color: asdf.color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 30,
          }}
        >
          {asdf.icon}
        </Box>

        <Box>
          <Typography variant="body2" color="gray">
            {label}
          </Typography>

          <Typography variant="h5">
            {value}
            {unit && (
              <Typography
                component="span"
                variant="body1"
                sx={{ opacity: 0.7, ml: 0.5 }}
              >
                {unit}
              </Typography>
            )}
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
}
