import { Box, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { displayFont } from '@/theme';

/** the amber of the last value, the glow of the cluster in the dark theme */
const AMBER = '#f5a524';

const CX = 120;
const CY = 140;
const R = 90;

/** the point of a value on the arc, from the left (0) to the right (1) */
function point(fraction: number, radius = R) {
  const angle = Math.PI * (1 - Math.min(1, Math.max(0, fraction)));
  return {
    x: CX + radius * Math.cos(angle),
    y: CY - radius * Math.sin(angle),
  };
}

const f = (value: number) => value.toFixed(1);

export interface GaugeProps {
  /** the average, the needle */
  value: number;
  /** the lowest and the highest value, the band on the arc */
  low?: number;
  high?: number;
  /** the latest value, a dot on the arc */
  last?: number;
  /** the ends of the scale */
  min: number;
  max: number;
  /** the value as shown, e.g. "6.01" */
  text: string;
  /** the unit below it, e.g. "l/100km" */
  unit: string;
  /** the whole reading for screen readers */
  label: string;
  /** e.g. low, high and last as text */
  caption?: ReactNode;
}

/**
 * A dial of the cluster: the average as the needle, the spread of the single
 * values as a band, the latest value as a dot
 */
export default function Gauge({
  value,
  low,
  high,
  last,
  min,
  max,
  text,
  unit,
  label,
  caption,
}: GaugeProps) {
  const span = max - min || 1;
  const at = (v: number) => (v - min) / span;
  const start = point(0);
  const end = point(1);
  const needleIn = point(at(value), R - 14);
  const needleOut = point(at(value), R + 14);
  const band =
    low !== undefined && high !== undefined && high > low
      ? { from: point(at(low)), to: point(at(high)) }
      : undefined;
  const dot = last !== undefined ? point(at(last)) : undefined;

  return (
    <Box
      component="figure"
      sx={{
        m: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        minWidth: 0,
      }}
    >
      <Box
        component="svg"
        role="img"
        aria-label={label}
        viewBox="0 0 240 160"
        sx={{ width: '100%', maxWidth: 250, height: 'auto', display: 'block' }}
      >
        <Box
          component="path"
          d={`M${f(start.x)} ${f(start.y)}A${R} ${R} 0 0 1 ${f(end.x)} ${f(end.y)}`}
          fill="none"
          strokeWidth={12}
          strokeLinecap="round"
          sx={{ stroke: (theme) => theme.vars.palette.divider }}
        />
        {band && (
          <Box
            component="path"
            d={`M${f(band.from.x)} ${f(band.from.y)}A${R} ${R} 0 0 1 ${f(band.to.x)} ${f(band.to.y)}`}
            fill="none"
            strokeWidth={12}
            sx={{
              stroke: (theme) => theme.vars.palette.primary.main,
              strokeOpacity: 0.4,
            }}
          />
        )}
        {dot && (
          <Box
            component="circle"
            cx={f(dot.x)}
            cy={f(dot.y)}
            r={5}
            sx={(theme) => ({
              fill: theme.vars.palette.warning.main,
              ...theme.applyStyles('dark', { fill: AMBER }),
            })}
          />
        )}
        <Box
          component="path"
          d={`M${f(needleIn.x)} ${f(needleIn.y)}L${f(needleOut.x)} ${f(needleOut.y)}`}
          strokeWidth={4}
          strokeLinecap="round"
          sx={{ stroke: (theme) => theme.vars.palette.text.primary }}
        />
        <Box
          component="text"
          x={CX}
          y={114}
          textAnchor="middle"
          sx={{
            fill: (theme) => theme.vars.palette.text.primary,
            fontFamily: displayFont,
            fontWeight: 700,
            fontSize: 38,
          }}
        >
          {text}
        </Box>
        <Box
          component="text"
          x={CX}
          y={140}
          textAnchor="middle"
          sx={{
            fill: (theme) => theme.vars.palette.text.secondary,
            // the dial shrinks on a phone: the unit stays readable
            fontSize: 18,
          }}
        >
          {unit}
        </Box>
      </Box>
      {caption && (
        <Typography
          component="figcaption"
          variant="body2"
          className="tabular"
          sx={{
            color: 'text.secondary',
            display: 'flex',
            flexWrap: 'wrap',
            justifyContent: 'center',
            columnGap: 1.75,
          }}
        >
          {caption}
        </Typography>
      )}
    </Box>
  );
}
