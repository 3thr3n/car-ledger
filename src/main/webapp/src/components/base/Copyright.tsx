import { Box, Link, Typography, useMediaQuery, useTheme } from '@mui/material';
import { useTranslation } from 'react-i18next';
import { TFunction } from 'i18next';

function getCopyright(t: TFunction<'translation', undefined>) {
  return (
    <>
      {'Copyright ©'}
      <Link target="_blank" href="https://www.codeflowwizardry.de">
        3thr3n
      </Link>{' '}
      {new Date().getFullYear()}{' '}
      <Link target="_blank" href="https://github.com/3thr3n/car-ledger">
        {t('app.title')}
      </Link>{' '}
    </>
  );
}

export default function Copyright() {
  const { t } = useTranslation();
  const theme = useTheme();
  const isSm = useMediaQuery(theme.breakpoints.down('sm'));

  return (
    <Box
      sx={{
        py: 1,
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
            {getCopyright(t)}
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
            {getCopyright(t)} — {t('app.slogan')}
          </Typography>
        </>
      )}
    </Box>
  );
}
