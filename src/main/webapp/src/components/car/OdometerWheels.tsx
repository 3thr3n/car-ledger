import { Box } from '@mui/material';
import { displayFont } from '@/theme';

/** the odometer as a row of digit wheels */
export default function OdometerWheels({
  value,
  label,
  size = 'medium',
}: {
  value: number;
  /** for screen readers, e.g. "Odometer 84,263 km" */
  label: string;
  /** small in a list, e.g. the garage */
  size?: 'small' | 'medium';
}) {
  const small = size === 'small';
  const digits = String(Math.round(value)).padStart(6, '0').split('');
  return (
    <Box
      role="img"
      aria-label={label}
      sx={{ display: 'flex', gap: '3px', alignItems: 'center' }}
    >
      {digits.map((digit, index) => (
        <Box
          // the places of the digits stay
          key={index}
          component="span"
          aria-hidden
          sx={(theme) => ({
            width: small ? 17 : 22,
            height: small ? 25 : 32,
            display: 'grid',
            placeItems: 'center',
            borderRadius: '4px',
            bgcolor: 'action.hover',
            fontFamily: displayFont,
            fontWeight: 600,
            fontSize: small ? 15 : 20,
            ...theme.applyStyles('dark', {
              bgcolor: '#0a1018',
              boxShadow: 'inset 0 -8px 10px -6px #000',
            }),
          })}
        >
          {digit}
        </Box>
      ))}
    </Box>
  );
}
