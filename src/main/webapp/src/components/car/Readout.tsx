import { Box, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { displayFont } from '@/theme';

/** a label with its value below, in a <dl> */
export default function Readout({
  label,
  value,
}: {
  label: ReactNode;
  value: ReactNode;
}) {
  return (
    <Box sx={{ textAlign: 'center', minWidth: 0 }}>
      <Typography
        component="dt"
        variant="body2"
        sx={{ color: 'text.secondary' }}
      >
        {label}
      </Typography>
      <Typography
        component="dd"
        className="tabular"
        sx={{
          m: 0,
          fontFamily: displayFont,
          fontWeight: 650,
          fontSize: { xs: '1.2rem', sm: '1.375rem' },
          whiteSpace: 'nowrap',
        }}
      >
        {value}
      </Typography>
    </Box>
  );
}
