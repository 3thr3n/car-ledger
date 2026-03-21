import { Box, Card, CardContent, Typography } from '@mui/material';
import { ReactNode } from 'react';
import RouteIcon from '@mui/icons-material/Route';
import LocalGasStationIcon from '@mui/icons-material/LocalGasStation';
import EuroIcon from '@mui/icons-material/Euro';
import { QuestionMark } from '@mui/icons-material';

export default function DashboardMetricHiLo({
  label,
  high,
  low,
  unit,
  type,
  invert,
}: {
  label: string;
  high: string;
  low: string;
  unit?: string;
  type: 'distance' | 'fuel' | 'cost' | 'div';
  invert?: boolean;
}) {
  const asdf: {
    icon?: ReactNode;
    color?: string;
    highColor: string;
    lowColor: string;
  } = {
    highColor: '#4caf50',
    lowColor: '#ef5350',
  };

  if (invert) {
    asdf.highColor = '#ef5350';
    asdf.lowColor = '#4caf50';
  }

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
        p: 1,
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
          alignItems: 'center',
          justifyContent: 'center',
          alignContent: 'center',
          justifyItems: 'center',
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
            flexShrink: 0,
          }}
        >
          {asdf.icon}
        </Box>

        <Box
          sx={{ display: 'flex', flexDirection: 'column', gap: 0.5, flex: 1 }}
        >
          <Typography variant="body2" color="gray">
            {label}
          </Typography>

          <Box
            display="flex"
            justifyContent="space-between"
            flexDirection={{ sm: 'column', md: 'row' }}
          >
            <Box>
              <Typography
                variant="caption"
                sx={{ color: asdf.highColor, opacity: 0.7 }}
              >
                HIGH
              </Typography>
              <Typography variant="h6">
                {high}
                {unit && (
                  <Typography
                    component="span"
                    variant="body2"
                    sx={{ opacity: 0.7, ml: 0.5 }}
                  >
                    {unit}
                  </Typography>
                )}
              </Typography>
            </Box>
            <Box>
              <Typography
                variant="caption"
                sx={{ color: asdf.lowColor, opacity: 0.7 }}
              >
                LOW
              </Typography>
              <Typography variant="h6">
                {low}
                {unit && (
                  <Typography
                    component="span"
                    variant="body2"
                    sx={{ opacity: 0.7, ml: 0.5 }}
                  >
                    {unit}
                  </Typography>
                )}
              </Typography>
            </Box>
          </Box>
        </Box>
      </CardContent>
    </Card>
  );
}
