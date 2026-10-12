import { Box, Stack, Typography } from '@mui/material';
import { ReactNode } from 'react';

export interface PageHeaderProps {
  title: ReactNode;
  eyebrow?: ReactNode;
  subtitle?: ReactNode;
  actions?: ReactNode;
}

export default function PageHeader({
  title,
  eyebrow,
  subtitle,
  actions,
}: PageHeaderProps) {
  return (
    <Stack
      direction={{ xs: 'column', sm: 'row' }}
      spacing={2}
      sx={{
        alignItems: { xs: 'stretch', sm: 'flex-end' },
        justifyContent: 'space-between',
        mb: 3,
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        {eyebrow && (
          <Typography
            variant="overline"
            component="div"
            sx={{
              color: 'text.secondary',
            }}
          >
            {eyebrow}
          </Typography>
        )}
        <Typography variant="h4" component="h1" noWrap>
          {title}
        </Typography>
        {subtitle && (
          <Typography
            sx={{
              color: 'text.secondary',
              mt: 0.5,
            }}
          >
            {subtitle}
          </Typography>
        )}
      </Box>
      {actions && (
        <Stack
          direction="row"
          spacing={1}
          sx={{ flexShrink: 0, flexWrap: 'wrap' }}
        >
          {actions}
        </Stack>
      )}
    </Stack>
  );
}
