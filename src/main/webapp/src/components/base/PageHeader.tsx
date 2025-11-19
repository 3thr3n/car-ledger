import { ArrowBack } from '@mui/icons-material';
import { Box, IconButton, Typography, TypographyVariant } from '@mui/material';
import { NavigateOptions } from '@tanstack/router-core';

export default function PageHeader({
  title,
  isMobile,
  navigateTo,
  navigate,
}: {
  title: string;
  isMobile?: boolean;
  navigateTo?: NavigateOptions;
  navigate: (path: NavigateOptions) => void;
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
      <Typography variant={variant} sx={{ pt: '2px' }}>
        {title}
      </Typography>
    </Box>
  );
}
