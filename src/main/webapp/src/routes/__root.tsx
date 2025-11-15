import Copyright from '@/components/base/Copyright';
import Navigation from '@/components/base/Navigation';

import theme from '@/utils/theme';
import queryClient from '@/utils/QueryClient';

import { Box, CssBaseline, ThemeProvider } from '@mui/material';

import { QueryClientProvider } from '@tanstack/react-query';
import { createRootRoute, Outlet, redirect } from '@tanstack/react-router';

import { Slide, ToastContainer } from 'react-toastify';

import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import 'dayjs/locale/de';

import 'react-toastify/dist/ReactToastify.css';
import CsvUploadDialog from '@/components/csv/CsvUploadDialog';
import ErrorPage from '@/pages/ErrorPage';
import NotFoundPage from '@/pages/NotFoundPage';

export const Route = createRootRoute({
  beforeLoad: (ctx) => {
    const { followRedirect }: { followRedirect?: string } = ctx.search;
    if (followRedirect == '') {
      const redirectUrl = localStorage.getItem('postLoginRedirect');
      if (redirectUrl) {
        localStorage.removeItem('postLoginRedirect');
        // redirect to original page
        throw redirect({ to: redirectUrl });
      }
    } else {
      const redirectUrl = localStorage.getItem('postLoginRedirect');
      if (redirectUrl) {
        localStorage.removeItem('postLoginRedirect');
      }
    }
  },
  component: () => (
    <>
      <ThemeProvider theme={theme}>
        <LocalizationProvider dateAdapter={AdapterDayjs} adapterLocale="de">
          <CssBaseline />
          <Navigation />
          <QueryClientProvider client={queryClient}>
            <CsvUploadDialog />
            <Box
              component="main"
              flexGrow={1}
              display="flex"
              flexDirection="column"
              overflow="auto"
              height="100%"
              px={{ xs: 2, sm: 4 }}
              py={3}
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
  errorComponent: () => <ErrorPage />,
  notFoundComponent: () => <NotFoundPage />,
});
