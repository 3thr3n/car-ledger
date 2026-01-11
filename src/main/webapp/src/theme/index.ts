import { createTheme } from '@mui/material/styles';
import { palette } from '@/theme/palette';
import { components } from '@/theme/components';
import { shape } from '@/theme/shape';
import { typography } from '@/theme/typography';
import { spacing } from '@/theme/spacing';

export const theme = createTheme({
  palette,
  typography,
  components,
  shape,
  spacing,
});

export default theme;
