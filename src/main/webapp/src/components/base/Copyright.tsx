import { Box, Link, Typography, useMediaQuery, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';

export default function Copyright() {
  const { t } = useTranslation();
  const theme = useTheme();
  const isSm = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Box
      sx={{
        pb: 1,
      }}
    >
      {isSm ? (
        <>
          <Typography
            variant="body2"
            align="center"
            sx={{
              color: 'text.secondary',
            }}
          >
            {'Copyright ©'}
            <Link href="https://www.codeflowwizardry.de">3thr3n</Link>{' '}
            {new Date().getFullYear()} {t('app.title')}
          </Typography>
          <Typography
            variant="body2"
            align="center"
            sx={{
              color: 'text.secondary',
            }}
          >
            {t('app.slogan')}
          </Typography>
        </>
      ) : (
        <>
          <Typography
            variant="body2"
            align="center"
            sx={{
              color: 'text.secondary',
            }}
          >
            {'Copyright ©'}
            <Link href="https://www.codeflowwizardry.de">3thr3n</Link>{' '}
            {new Date().getFullYear()} {t('app.title')} — {t('app.slogan')}
          </Typography>
        </>
      )}
    </Box>
  );
}
