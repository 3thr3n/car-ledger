import { Box, Container } from '@mui/material';
import { ReactNode } from 'react';

export interface PageProps {
  id: string;
  children: ReactNode;
  maxWidth?: 'sm' | 'md' | 'lg' | 'xl';
}

/**
 * Content area of a page inside the app shell
 */
export default function Page({ id, children, maxWidth = 'lg' }: PageProps) {
  return (
    <Box id={id} sx={{ flexGrow: 1, py: { xs: 3, md: 5 } }}>
      <Container maxWidth={maxWidth}>{children}</Container>
    </Box>
  );
}
