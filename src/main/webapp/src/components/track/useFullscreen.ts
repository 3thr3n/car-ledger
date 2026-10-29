import { SxProps, Theme } from '@mui/material';
import { useCallback, useEffect, useState } from 'react';

/**
 * The area of a map in fullscreen, with what belongs to it (the period, the
 * legend, the places): the area covers the window ({@link fullscreenSx}), and
 * the page goes into the fullscreen of the browser where it has one (not
 * Safari on the iPhone). The whole page, not the area: the dialogs and pickers
 * open outside of it and must stay visible. The same button or Esc returns.
 */
export default function useFullscreen() {
  const [fullscreen, setFullscreen] = useState(false);

  const leave = useCallback(() => {
    setFullscreen(false);
    if (document.fullscreenElement) {
      void document.exitFullscreen().catch(() => undefined);
    }
  }, []);

  const toggle = useCallback(() => {
    if (fullscreen) {
      leave();
      return;
    }
    setFullscreen(true);
    // the window is covered anyway, the API hides the bars of the browser too
    void document.documentElement.requestFullscreen?.().catch(() => undefined);
  }, [fullscreen, leave]);

  // left with Esc or by the browser
  useEffect(() => {
    const changed = () => {
      if (!document.fullscreenElement) setFullscreen(false);
    };
    document.addEventListener('fullscreenchange', changed);
    return () => document.removeEventListener('fullscreenchange', changed);
  }, []);

  // Esc over the window, where the browser has no fullscreen of its own
  useEffect(() => {
    if (!fullscreen) return;
    const key = (event: KeyboardEvent) => {
      if (event.key === 'Escape') leave();
    };
    window.addEventListener('keydown', key);
    return () => window.removeEventListener('keydown', key);
  }, [fullscreen, leave]);

  return { fullscreen, toggle };
}

/** the area covers the window, the map takes the height left */
export const fullscreenSx: SxProps<Theme> = {
  position: 'fixed',
  inset: 0,
  zIndex: (theme) => theme.zIndex.modal,
  bgcolor: 'background.default',
  p: { xs: 1.5, sm: 3 },
  overflowY: 'auto',
  display: 'flex',
  flexDirection: 'column',
};
