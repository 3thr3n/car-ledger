import {
  createTheme,
  PaletteColor,
  PaletteColorChannel,
} from '@mui/material/styles';
import type {} from '@mui/material/themeCssVarsAugmentation';
import type {} from '@mui/x-data-grid/themeAugmentation';
import type {} from '@mui/x-date-pickers/themeAugmentation';

declare module '@mui/material/styles' {
  // channels are generated for every color, they are used for tinted backgrounds
  interface Palette {
    fuel: PaletteColor & PaletteColorChannel;
    maintenance: PaletteColor & PaletteColorChannel;
    miscellaneous: PaletteColor & PaletteColorChannel;
    recurring: PaletteColor & PaletteColorChannel;
  }

  interface PaletteOptions {
    fuel?: PaletteOptions['primary'];
    maintenance?: PaletteOptions['primary'];
    miscellaneous?: PaletteOptions['primary'];
    recurring?: PaletteOptions['primary'];
  }
}

const fontFamily = `"Inter Variable", "Inter", system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`;

export const theme = createTheme({
  cssVariables: {
    colorSchemeSelector: 'class',
  },
  colorSchemes: {
    light: {
      palette: {
        primary: { main: '#0f766e', contrastText: '#ffffff' },
        secondary: { main: '#475569' },
        background: { default: '#f5f6f8', paper: '#ffffff' },
        text: { primary: '#111827', secondary: '#5b6472' },
        divider: '#e4e7ec',
        fuel: { main: '#d97706' },
        maintenance: { main: '#2563eb' },
        miscellaneous: { main: '#9333ea' },
        recurring: { main: '#db2777' },
      },
    },
    dark: {
      palette: {
        primary: { main: '#2dd4bf', contrastText: '#04201d' },
        secondary: { main: '#94a3b8' },
        background: { default: '#0d1014', paper: '#151a20' },
        text: { primary: '#e6e9ee', secondary: '#9aa3af' },
        divider: '#252c35',
        fuel: { main: '#fbbf24' },
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
          '& .MuiDataGrid-cell:focus, & .MuiDataGrid-columnHeader:focus': {
            outline: 'none',
          },
        }),
      },
    },
  },
});

export default theme;
