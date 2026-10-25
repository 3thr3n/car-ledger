import { accentTheme } from '@/theme';
import queryClient from '@/utils/QueryClient';

import { CssBaseline, ThemeProvider, useColorScheme } from '@mui/material';

import { QueryClientProvider } from '@tanstack/react-query';
import { createRootRoute, Outlet, redirect } from '@tanstack/react-router';

import { Slide, ToastContainer } from 'react-toastify';

import { LocalizationProvider } from '@mui/x-date-pickers';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import 'dayjs/locale/de';
import 'dayjs/locale/en';

import 'react-toastify/dist/ReactToastify.css';
import ErrorPage from '@/pages/ErrorPage';
import NotFoundPage from '@/pages/NotFoundPage';
import AppShell from '@/components/layout/AppShell';
import { useTranslation } from 'react-i18next';
import { ReactNode } from 'react';
import useAccent from '@/hooks/useAccent';

export const Route = createRootRoute({
  beforeLoad: (ctx) => {
    const { followRedirect }: { followRedirect?: string } = ctx.search;
    const redirectUrl = localStorage.getItem('postLoginRedirect');

    // beforeLoad can run several times for the same navigation, so the target is
    // only cleared on the next regular navigation
    if (followRedirect !== '') {
      if (redirectUrl) localStorage.removeItem('postLoginRedirect');
      return;
    }

    // back from the login, continue where the user left (only paths of this app)
    if (redirectUrl?.startsWith('/') && !redirectUrl.startsWith('//')) {
      const target = new URL(redirectUrl, location.origin);
      throw redirect({
        to: target.pathname,
        search: Object.fromEntries(target.searchParams),
        replace: true,
      });
    }
    throw redirect({ to: '/dashboard', replace: true });
  },
  component: Root,
  errorComponent: ({ error }) => (
    <Providers>
      <ErrorPage error={error} />
    </Providers>
  ),
  notFoundComponent: () => <NotFoundPage />,
});

function Providers({ children }: { children: ReactNode }) {
  // the accent comes with the account
  return (
    <QueryClientProvider client={queryClient}>
      <Themed>{children}</Themed>
    </QueryClientProvider>
  );
}

function Themed({ children }: { children: ReactNode }) {
  const { i18n } = useTranslation();
  const accent = useAccent();

  return (
    <ThemeProvider theme={accentTheme(accent)} defaultMode="system">
      <LocalizationProvider
        dateAdapter={AdapterDayjs}
        adapterLocale={i18n.language === 'DE' ? 'de' : 'en'}
      >
        <CssBaseline />
        <AppShell>{children}</AppShell>
        <Toasts />
      </LocalizationProvider>
    </ThemeProvider>
  );
}

function Toasts() {
  const { mode, systemMode } = useColorScheme();
  const resolved = mode === 'system' ? systemMode : mode;

  return (
    <ToastContainer
      autoClose={3000}
      position="bottom-right"
      pauseOnHover
      transition={Slide}
      theme={resolved === 'dark' ? 'dark' : 'light'}
    />
  );
}

function Root() {
  return (
    <Providers>
      <Outlet />
    </Providers>
  );
}
