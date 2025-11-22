import { Box, Card, CardContent, Typography } from '@mui/material';
import { ReactNode } from 'react';

export default function DashboardMetric({
  label,
  value,
  unit,
  icon,
  color = '#4bc0c0',
}: {
  label: string;
  value: string | number;
  unit?: string;
  icon: ReactNode;
  color?: string;
}) {
  return (
    <Card
      sx={{
        p: 2,
        borderRadius: 4,
        background: '#1a1a1a',
        border: '1px solid #2a2a2a',
        minHeight: 120,
        transition: '0.2s ease',
        ':hover': {
          background: '#202020',
          borderColor: color,
          transform: 'translateY(-3px)',
        },
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
            background: color + '20',
            color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: 30,
          }}
        >
          {icon}
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
