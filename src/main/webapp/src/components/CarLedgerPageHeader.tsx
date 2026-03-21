import { ArrowBack } from '@mui/icons-material';
import { Box, IconButton, Typography, TypographyVariant } from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';
import { ReactNode } from 'react';
import { useRouter } from '@tanstack/react-router';

export default function CarLedgerPageHeader({
  title,
  isMobile,
  children,
}: {
  title: string;
  isMobile?: boolean;
  navigateTo?: NavigateOptions;
  navigate?: (path: NavigateOptions) => void;
  goToPreviousPage?: boolean;
  children?: ReactNode;
}) {
  const router = useRouter();
  let variant: TypographyVariant = 'h4';

  if (isMobile) {
    variant = 'h6';
  }

  const nav = () => {
    router.history.back();
  };

  return (
    <Box my={0} display={'flex'} mb={2}>
      <IconButton onClick={nav} sx={{ mr: 1 }}>
        <ArrowBack fontSize="large" />
      </IconButton>
      <Typography variant={variant} sx={{ pt: 1 }}>
        {title}
      </Typography>
      {children}
    </Box>
  );
}
