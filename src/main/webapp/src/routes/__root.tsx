import Copyright from '@/components/base/Copyright';
import Navigation from '@/components/base/Navigation';

import theme from '@/theme';
import queryClient from '@/utils/QueryClient';

import { Box, CssBaseline, ThemeProvider } from '@mui/material';

import { QueryClientProvider } from '@tanstack/react-query';
import { createRootRoute, Outlet, redirect } from '@tanstack/react-router';

import { Slide, ToastContainer } from 'react-toastify';

import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import 'dayjs/locale/de';

import 'react-toastify/dist/ReactToastify.css';
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
      throw redirect({ to: '/dashboard', replace: true });
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
          <QueryClientProvider client={queryClient}>
            <Navigation />
            <Box
              id="RootBox"
              component="main"
              flexGrow={1}
              display="flex"
              flexDirection="column"
              overflow="hidden"
              height="100%"
            >
              <Outlet />
            </Box>
            <Copyright />
          </QueryClientProvider>
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
