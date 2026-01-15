import { Components } from '@mui/material';

export const components: Components = {
  MuiButton: {
    defaultProps: {
      disableElevation: true,
    },
    styleOverrides: {
      root: {
        borderRadius: 8,
        textTransform: 'none',
        margin: 1,
      },
    },
  },
  MuiCard: {
    styleOverrides: {
      root: {
        borderRadius: 12,
      },
    },
  },

  MuiDialog: {
    styleOverrides: {
      paper: {
        borderRadius: 12,
      },
    },
  },
  MuiAlert: {
    styleOverrides: {
      root: {
        variants: [
          {
            props: { severity: 'info' },
            style: {
              backgroundColor: '#60a5fa',
            },
          },
        ],
      },
    },
  },
};
