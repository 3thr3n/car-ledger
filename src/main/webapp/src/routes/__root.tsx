import Copyright from '@/components/base/Copyright';
import Navigation from '@/components/base/Navigation';

import theme from '@/utils/theme';
import queryClient from '@/utils/QueryClient';

import { Box, CssBaseline, ThemeProvider } from '@mui/material';

import { QueryClientProvider } from '@tanstack/react-query';
import { createRootRoute, Outlet } from '@tanstack/react-router';

import { Slide, ToastContainer } from 'react-toastify';

import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import 'dayjs/locale/de';

import 'react-toastify/dist/ReactToastify.css';
import CsvUploadDialog from '@/components/csv/CsvUploadDialog';

export const Route = createRootRoute({
  component: () => (
    <>
      <ThemeProvider theme={theme}>
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="de">
          <CssBaseline />
          <Navigation />
          <QueryClientProvider client={queryClient}>
            <CsvUploadDialog />
            <Box
              flexGrow={1}
              display="flex"
              flexDirection="column"
              justifyContent="center"
              alignItems="center"
              marginY={2}
              overflow="auto"
              position="relative"
            >
              <Outlet />
            </Box>
          </QueryClientProvider>
          <Copyright />
          <ToastContainer
            autoClose={2500}
            position="bottom-left"
            pauseOnHover
            transition={Slide}
            theme="colored"
          />
        </LocalizationProvider>
      </ThemeProvider>
    </>
  ),
  errorComponent: () => (
    <div>Nothing to see here, the frontend is broken please reload</div>
  ),
});
