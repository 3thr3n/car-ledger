import { Box, Typography } from '@mui/material';
import { ReactNode } from 'react';

/**
 * One setting: the control and, next to it, what it does. On a phone the
 * explanation is below the control.
 */
export default function SettingRow({
  children,
  help,
}: {
  children: ReactNode;
  help?: ReactNode;
}) {
  return (
    <Box
      sx={{
        display: 'grid',
        gridTemplateColumns: {
          xs: '1fr',
          md: 'minmax(0, 20rem) minmax(0, 1fr)',
        },
        columnGap: 3,
        rowGap: 0.75,
        alignItems: 'center',
        py: 1.75,
        '& + &': { borderTop: 1, borderColor: 'divider' },
      }}
    >
      <Box sx={{ minWidth: 0 }}>{children}</Box>
      {help && (
        <Typography
          variant="body2"
          sx={{ color: 'text.secondary', maxWidth: '38rem' }}
        >
          {help}
        </Typography>
      )}
    </Box>
  );
}
