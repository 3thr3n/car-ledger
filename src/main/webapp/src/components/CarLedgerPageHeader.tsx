import { ArrowBack } from '@mui/icons-material';
import { Box, IconButton, Typography, TypographyVariant } from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';
import { ReactNode } from 'react';

export default function CarLedgerPageHeader({
  title,
  isMobile,
  navigateTo,
  navigate,
  children,
}: {
  title: string;
  isMobile?: boolean;
  navigateTo?: NavigateOptions;
  navigate: (path: NavigateOptions) => void;
  children?: ReactNode;
}) {
  let variant: TypographyVariant = 'h4';

  if (isMobile) {
    variant = 'h6';
  }

  return (
    <Box my={0} display={'flex'} mb={2}>
      <IconButton
        onClick={() => navigate(navigateTo ?? { to: '..' })}
        sx={{ mr: 1 }}
      >
        <ArrowBack fontSize="large" />
      </IconButton>
      <Typography variant={variant} sx={{ pt: 1 }}>
        {title}
      </Typography>
      {children}
    </Box>
  );
}
