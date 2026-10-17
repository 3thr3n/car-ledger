import { Box, Card, Stack, SxProps, Theme, Typography } from '@mui/material';
import { ReactNode } from 'react';

export interface SectionProps {
  title?: ReactNode;
  subtitle?: ReactNode;
  action?: ReactNode;
  children: ReactNode;
  /** remove the content padding, e.g. for tables */
  flush?: boolean;
  sx?: SxProps<Theme>;
}

export default function Section({
  title,
  subtitle,
  action,
  children,
  flush,
  sx,
}: SectionProps) {
  return (
    <Card sx={sx}>
      {(title || action) && (
        <Stack
          direction="row"
          spacing={2}
          sx={{
            alignItems: 'center',
            px: 2.5,
            pt: 2,
            pb: flush ? 1 : 0,
          }}
        >
          <Box sx={{ flexGrow: 1, minWidth: 0 }}>
            {title && <Typography variant="h6">{title}</Typography>}
            {subtitle && (
              <Typography
                variant="body2"
                sx={{
                  color: 'text.secondary',
                }}
              >
                {subtitle}
              </Typography>
            )}
          </Box>
          {action}
        </Stack>
      )}
      <Box sx={{ p: flush ? 0 : 2.5 }}>{children}</Box>
    </Card>
  );
}
