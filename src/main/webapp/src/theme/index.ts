import {
  createTheme,
  PaletteColor,
  PaletteColorChannel,
} from '@mui/material/styles';
import type {} from '@mui/material/themeCssVarsAugmentation';
import type {} from '@mui/x-data-grid/themeAugmentation';
import type {} from '@mui/x-date-pickers/themeAugmentation';
import { Accent } from '@/generated';

declare module '@mui/material/styles' {
  // channels are generated for every color, they are used for tinted backgrounds
  interface Palette {
    fuel: PaletteColor & PaletteColorChannel;
    charging: PaletteColor & PaletteColorChannel;
    maintenance: PaletteColor & PaletteColorChannel;
    miscellaneous: PaletteColor & PaletteColorChannel;
    recurring: PaletteColor & PaletteColorChannel;
  }

  interface PaletteOptions {
    fuel?: PaletteOptions['primary'];
    charging?: PaletteOptions['primary'];
    maintenance?: PaletteOptions['primary'];
    miscellaneous?: PaletteOptions['primary'];
    recurring?: PaletteOptions['primary'];
  }
}

const fontFamily = `"Inter Variable", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`;

/**
 * The presets of the accent color: buttons, links, the active tab and the
 * highlights. Each has its own color for light and dark, both readable as text
 * on the background and with their contrast text on top (at least 4.5:1). The
 * colors of the categories stay, the charts keep their meaning.
 */
export const ACCENTS: Record<
  Accent,
  Record<'light' | 'dark', { main: string; contrastText: string }>
> = {
  [Accent.TEAL]: {
    light: { main: '#0f766e', contrastText: '#ffffff' },
    dark: { main: '#2dd4bf', contrastText: '#04201d' },
  },
  [Accent.BLUE]: {
    light: { main: '#1d4ed8', contrastText: '#ffffff' },
    dark: { main: '#60a5fa', contrastText: '#03142e' },
  },
  [Accent.VIOLET]: {
    light: { main: '#6d28d9', contrastText: '#ffffff' },
    dark: { main: '#a78bfa', contrastText: '#1a0b3d' },
  },
  [Accent.ROSE]: {
    light: { main: '#be123c', contrastText: '#ffffff' },
    dark: { main: '#fb7185', contrastText: '#3b0613' },
  },
  [Accent.AMBER]: {
    light: { main: '#b45309', contrastText: '#ffffff' },
    dark: { main: '#fbbf24', contrastText: '#2a1700' },
  },
};

const createAppTheme = (accent: Accent) =>
  createTheme({
    cssVariables: {
      colorSchemeSelector: 'class',
    },
    colorSchemes: {
      light: {
        palette: {
          primary: ACCENTS[accent].light,
          secondary: { main: '#475569' },
          // darker than the defaults of MUI: white on them and they as text reach 4.5:1
          warning: { main: '#b45309' },
          info: { main: '#0277bd' },
          background: { default: '#f5f6f8', paper: '#ffffff' },
          text: { primary: '#111827', secondary: '#5b6472' },
          divider: '#e4e7ec',
          fuel: { main: '#d97706' },
          charging: { main: '#16a34a' },
          maintenance: { main: '#2563eb' },
          miscellaneous: { main: '#9333ea' },
          recurring: { main: '#db2777' },
        },
      },
      dark: {
        palette: {
          primary: ACCENTS[accent].dark,
          secondary: { main: '#94a3b8' },
          background: { default: '#0d1014', paper: '#151a20' },
          text: { primary: '#e6e9ee', secondary: '#9aa3af' },
          divider: '#252c35',
          fuel: { main: '#fbbf24' },
          charging: { main: '#4ade80' },
          maintenance: { main: '#60a5fa' },
          miscellaneous: { main: '#c084fc' },
          recurring: { main: '#f472b6' },
        },
      },
    },
    shape: {
      borderRadius: 10,
    },
    typography: {
      fontFamily,
      h1: { fontWeight: 700, letterSpacing: '-0.02em' },
      h2: { fontWeight: 700, letterSpacing: '-0.02em' },
      h3: { fontWeight: 700, letterSpacing: '-0.02em' },
      h4: { fontWeight: 700, letterSpacing: '-0.015em', fontSize: '1.75rem' },
      h5: { fontWeight: 650, letterSpacing: '-0.01em' },
      h6: { fontWeight: 650, fontSize: '1.05rem' },
      subtitle2: { fontWeight: 600 },
      overline: { fontWeight: 600, letterSpacing: '0.08em', lineHeight: 1.6 },
      button: { fontWeight: 600, textTransform: 'none' },
    },
    components: {
      // the levels of the headings follow the page: its title h1 (PageHeader), the sections h2, the groups within
      // them and in dialogs h3; a text in such a variant that isn't a heading sets its own component
      MuiTypography: {
        defaultProps: {
          variantMapping: {
            h5: 'h2',
            h6: 'h2',
            subtitle1: 'h3',
            subtitle2: 'h3',
          },
        },
      },
      MuiCssBaseline: {
        styleOverrides: {
          body: {
            fontFeatureSettings: '"cv11", "ss01"',
            WebkitFontSmoothing: 'antialiased',
          },
          '.tabular': {
            fontVariantNumeric: 'tabular-nums',
          },
        },
      },
      MuiButton: {
        defaultProps: { disableElevation: true },
        styleOverrides: {
          root: { borderRadius: 8 },
        },
      },
      MuiPaper: {
        defaultProps: { elevation: 0 },
      },
      MuiCard: {
        defaultProps: { variant: 'outlined' },
        styleOverrides: {
          root: ({ theme }) => ({
            borderRadius: 14,
            borderColor: theme.vars.palette.divider,
          }),
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600 },
        },
      },
      MuiTab: {
        styleOverrides: {
          root: { minHeight: 44, fontWeight: 600 },
        },
      },
      MuiTabs: {
        styleOverrides: {
          root: { minHeight: 44 },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: { borderRadius: 16 },
        },
      },
      MuiTextField: {
        defaultProps: { fullWidth: true },
      },
      MuiDataGrid: {
        styleOverrides: {
          root: ({ theme }) => ({
            border: 'none',
            fontVariantNumeric: 'tabular-nums',
            '--DataGrid-containerBackground': 'transparent',
            '& .MuiDataGrid-columnHeaderTitle': {
              fontWeight: 600,
              color: theme.vars.palette.text.secondary,
            },
            '& .MuiDataGrid-row:hover': {
              backgroundColor: `rgba(${theme.vars.palette.primary.mainChannel} / 0.05)`,
            },
            '& .strong': {
              fontWeight: 600,
            },
            // the focus of a click isn't shown, the one of the keyboard is
            '& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus': {
              outline: 'none',
            },
            '& .MuiDataGrid-cell:focus-visible, & .MuiDataGrid-columnHeader:focus-visible':
              {
                outline: `2px solid ${theme.vars.palette.primary.main}`,
                outlineOffset: -2,
              },
          }),
        },
      },
    },
  });

const themes = new Map<Accent, ReturnType<typeof createAppTheme>>();

/** the theme of the accent, created once */
export function accentTheme(accent: Accent) {
  let theme = themes.get(accent);
  if (!theme) {
    theme = createAppTheme(accent);
    themes.set(accent, theme);
  }
  return theme;
}
