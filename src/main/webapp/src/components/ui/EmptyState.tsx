import { Box, Stack, Typography } from '@mui/material';
import { ReactNode } from 'react';

export interface EmptyStateProps {
  icon?: ReactNode;
  title: ReactNode;
  text?: ReactNode;
  action?: ReactNode;
  compact?: boolean;
}

export default function EmptyState({
  icon,
  title,
  text,
  action,
  compact,
}: EmptyStateProps) {
  return (
    <Stack
      spacing={1}
      sx={{
        alignItems: 'center',
        textAlign: 'center',
        py: compact ? 3 : 6,
        px: 2,
      }}
    >
      {icon && (
        <Box
          sx={{
            color: 'text.secondary',
            opacity: 0.6,
            mb: 0.5,
            '& svg': { fontSize: compact ? 32 : 44 },
          }}
        >
          {icon}
        </Box>
      )}
      <Typography
        variant="subtitle1"
        component="h2"
        sx={{
          fontWeight: 600,
        }}
      >
        {title}
      </Typography>
      {text && (
        <Typography
          variant="body2"
          sx={{
            color: 'text.secondary',
            maxWidth: 420,
          }}
        >
          {text}
        </Typography>
      )}
      {action && <Box sx={{ pt: 1 }}>{action}</Box>}
    </Stack>
  );
}
