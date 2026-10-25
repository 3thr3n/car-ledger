import { Box, Typography } from '@mui/material';
import type { ReactNode } from 'react';

export interface ScaleTick {
  /** where on the bar, 0 (start) to 1 (end) */
  at: number;
  label: ReactNode;
}

/**
 * A color scale of the track: its title, the gradient and the ticks below,
 * then the children, e.g. a hint
 */
export default function ScaleLegend({
  title,
  label,
  gradient,
  ticks,
  testId,
  children,
}: {
  title: string;
  /** of the bar, for screen readers */
  label: string;
  /** CSS background of the bar */
  gradient: string;
  ticks: ScaleTick[];
  testId?: string;
  children?: ReactNode;
}) {
  return (
    <Box sx={{ flexGrow: 1, maxWidth: 360 }} data-testid={testId}>
      <Typography variant="caption" sx={{ color: 'text.secondary' }}>
        {title}
      </Typography>
      <Box
        role="img"
        aria-label={label}
        sx={{ height: 8, borderRadius: 4, background: gradient, mt: 0.5 }}
      />
      <Box sx={{ position: 'relative', height: 18 }}>
        {ticks.map(({ at, label: tick }) => (
          <Typography
            key={at}
            variant="caption"
            sx={{
              position: 'absolute',
              left: `${at * 100}%`,
              // the outer ticks stay within the bar
              transform:
                at === 0
                  ? 'none'
                  : at === 1
                    ? 'translateX(-100%)'
                    : 'translateX(-50%)',
              color: 'text.secondary',
            }}
          >
            {tick}
          </Typography>
        ))}
      </Box>
      {children}
    </Box>
  );
}
