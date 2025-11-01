import { Box, Link, Typography, useMediaQuery, useTheme } from '@mui/material';

export default function Copyright() {
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
            {new Date().getFullYear()}
            {' CarLedger'}
          </Typography>
          <Typography
            variant="body2"
            align="center"
            sx={{
              color: 'text.secondary',
            }}
          >
            Collect fuel bills. Plan maintenance. Know your costs.
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
            {new Date().getFullYear()} CarLedger — Collect fuel bills. Plan
            maintenance. Know your costs.
          </Typography>
        </>
      )}
    </Box>
  );
}
