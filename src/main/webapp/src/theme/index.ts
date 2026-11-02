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

/**
 * The typefaces of the two looks: the light one follows road signs (Overpass for
 * the figures and headlines, Inter for the text), the dark one an instrument
 * cluster (Barlow Semi Condensed and Barlow). Both are CSS variables of the color
 * scheme, so a component names the role, not the face.
 */
const fonts = {
  light: {
    text: `"Inter Variable", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`,
    display: `"Overpass Variable", "Inter Variable", system-ui, sans-serif`,
  },
  dark: {
    text: `"Barlow", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`,
    display: `"Barlow Semi Condensed", "Barlow", system-ui, sans-serif`,
  },
};

/** the face of the figures and headlines, in the color scheme shown */
export const displayFont = 'var(--app-font-display)';

const fontFamily = 'var(--app-font-text)';

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
          // road grey on light grey, white for the parts to work with
          background: { default: '#f3f4f6', paper: '#ffffff' },
          text: { primary: '#2b2f36', secondary: '#5b6472' },
          divider: '#d9dce1',
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
          // lighter than the default of MUI: as text on the panels at least 4.5:1
          error: { main: '#f87171' },
          // the night of a cluster: panels lit a little above the ground
          background: { default: '#0e1620', paper: '#17212d' },
          text: { primary: '#e6edf3', secondary: '#93a1b2' },
          divider: '#263445',
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
      h1: {
        fontFamily: displayFont,
        fontWeight: 700,
        letterSpacing: '-0.02em',
      },
      h2: {
        fontFamily: displayFont,
        fontWeight: 700,
        letterSpacing: '-0.02em',
      },
      h3: {
        fontFamily: displayFont,
        fontWeight: 700,
        letterSpacing: '-0.02em',
      },
      h4: {
        fontFamily: displayFont,
        fontWeight: 650,
        letterSpacing: '-0.01em',
        fontSize: '2rem',
      },
      h5: { fontWeight: 650, letterSpacing: '-0.01em' },
      h6: { fontWeight: 650, fontSize: '1.05rem' },
      subtitle2: { fontWeight: 600 },
      // a small label in sentence case, not the tracked capitals of the default
      overline: {
        fontWeight: 600,
        fontSize: '0.8125rem',
        letterSpacing: 0,
        lineHeight: 1.6,
        textTransform: 'none',
      },
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
        styleOverrides: (theme) => ({
          // on the root, so the portals of dialogs and menus get them too;
          // the class of the dark scheme sits on the root itself
          ':root': {
            '--app-font-text': fonts.light.text,
            '--app-font-display': fonts.light.display,
          },
          [theme.getColorSchemeSelector('dark').replace(/\s*&$/, '')]: {
            '--app-font-text': fonts.dark.text,
            '--app-font-display': fonts.dark.display,
          },
          body: {
            // the alternative digits of Inter, Barlow has no such features
            fontFeatureSettings: '"cv11", "ss01"',
            WebkitFontSmoothing: 'antialiased',
          },
          '.tabular': {
            fontVariantNumeric: 'tabular-nums',
          },
        }),
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
          // light: white with a border on the grey page; dark: a lit panel
          // without one, rounder like the glass of a cluster
          root: ({ theme }) => ({
            borderRadius: 10,
            borderColor: theme.vars.palette.divider,
            ...theme.applyStyles('dark', {
              borderRadius: 16,
              borderColor: 'transparent',
            }),
          }),
        },
      },
      MuiChip: {
        styleOverrides: {
          root: { fontWeight: 600 },
        },
      },
      // the default is the text color at 54 %, too faint on the grey page
      MuiToggleButton: {
        styleOverrides: {
          root: ({ theme }) => ({ color: theme.vars.palette.text.secondary }),
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
