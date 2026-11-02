import { Box, InputBase, Theme, Typography } from '@mui/material';
import { ReactNode, useId } from 'react';
import { useTranslation } from 'react-i18next';
import { NumericFormat } from 'react-number-format';
import { CLUSTER_AMBER, displayFont } from '@/theme';

/** the dark glass of a pump, sunk into the panel; the ring in the color */
const darkGlass = (ring: string, width = 1) => ({
  bgcolor: '#0a1018',
  borderColor: 'transparent',
  boxShadow: `inset 0 2px 10px rgba(0 0 0 / 0.6), 0 0 0 ${width}px ${ring}`,
});

/** the glass of the display: a bordered panel, the dark glass in the dark */
const glassSx = (theme: Theme) => ({
  px: 2,
  borderRadius: '10px',
  border: 1,
  borderColor: 'divider',
  ...theme.applyStyles('dark', darkGlass(theme.vars.palette.divider)),
});

/** a line of the display, a rule between two */
const lineSx = {
  display: 'grid',
  alignItems: 'center',
  columnGap: 2,
  '& + &': { borderTop: 1, borderColor: 'divider' },
} as const;

/** the figure of a line, lit amber in the dark */
const figureSx = (theme: Theme) => ({
  fontFamily: displayFont,
  fontWeight: 700,
  lineHeight: 1.2,
  fontVariantNumeric: 'tabular-nums',
  textAlign: 'right' as const,
  ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
});

/** a line of a pump that only shows: what it counts and the figure */
export function PumpReading({
  label,
  value,
}: {
  label: ReactNode;
  value: ReactNode;
}) {
  return (
    <Box
      sx={{ ...lineSx, gridTemplateColumns: 'minmax(0, 1fr) auto', py: 0.75 }}
    >
      <Typography component="dt" sx={{ color: 'text.secondary' }}>
        {label}
      </Typography>
      <Typography
        component="dd"
        sx={(theme) => ({
          ...figureSx(theme),
          m: 0,
          fontSize: { xs: '1.5rem', sm: '1.75rem' },
          whiteSpace: 'nowrap',
        })}
      >
        {value}
      </Typography>
    </Box>
  );
}

/** the display of a pump to read, its lines {@link PumpReading} */
export function PumpReadings({ children }: { children: ReactNode }) {
  return (
    <Box component="dl" sx={(theme) => ({ ...glassSx(theme), m: 0, py: 0.75 })}>
      {children}
    </Box>
  );
}

export interface PumpLineProps {
  label: string;
  value?: number;
  onChange: (value: number | undefined) => void;
  /** after the figure, e.g. " €" or " l" */
  suffix: string;
  decimalScale?: number;
  max?: number;
  /** the value calculated from the other two lines, shown while empty */
  computed?: string;
  /** below the figure, e.g. "Calculated: 175.0 ct/l" */
  helperText?: ReactNode;
}

/**
 * A line of the pump to type into: what it counts, and the figure as large as
 * on the display; the value calculated from the others shows dimmed while
 * the line is empty
 */
export function PumpLine({
  label,
  value,
  onChange,
  suffix,
  decimalScale = 2,
  max,
  computed,
  helperText,
}: PumpLineProps) {
  const { i18n } = useTranslation();
  const german = i18n.language === 'DE';
  const id = useId();

  return (
    <Box
      sx={{
        ...lineSx,
        gridTemplateColumns: 'minmax(0, auto) minmax(0, 1fr)',
        py: 1,
      }}
    >
      <Typography
        component="label"
        htmlFor={id}
        sx={{ color: 'text.secondary', cursor: 'text' }}
      >
        {label}
      </Typography>
      <NumericFormat
        customInput={InputBase}
        id={id}
        value={value ?? ''}
        onValueChange={(values) => onChange(values.floatValue)}
        decimalScale={decimalScale}
        decimalSeparator={german ? ',' : '.'}
        thousandSeparator={german ? '.' : ','}
        allowedDecimalSeparators={[',', '.']}
        allowNegative={false}
        suffix={suffix}
        isAllowed={({ floatValue }) =>
          max === undefined || (floatValue ?? 0) <= max
        }
        onFocus={(event) => event.target.select()}
        placeholder={computed ?? `0${suffix}`}
        inputMode="decimal"
        inputProps={{
          'aria-describedby': helperText ? `${id}-help` : undefined,
        }}
        sx={(theme) => ({
          '& input': {
            ...figureSx(theme),
            p: 0,
            fontSize: { xs: '1.625rem', sm: '1.875rem' },
            '&::placeholder': {
              color: 'text.secondary',
              opacity: computed ? 0.85 : 0.4,
            },
          },
        })}
      />
      {helperText && (
        <Typography
          id={`${id}-help`}
          variant="caption"
          sx={{ gridColumn: 2, textAlign: 'right', color: 'text.secondary' }}
        >
          {helperText}
        </Typography>
      )}
    </Box>
  );
}

/**
 * The display of a pump to type a refuel or a charge into: total, amount and
 * price per unit, two of them are enough
 *
 * @param error shown below, the glass turns red
 */
export default function Pump({
  label,
  error,
  children,
}: {
  /** of the group, for screen readers */
  label: string;
  error?: string;
  children: ReactNode;
}) {
  const errorId = useId();
  return (
    <Box>
      <Box
        role="group"
        aria-label={label}
        aria-describedby={error ? errorId : undefined}
        sx={(theme) => {
          const ring = error
            ? theme.vars.palette.error.main
            : theme.vars.palette.primary.main;
          return {
            ...glassSx(theme),
            py: 0.5,
            ...(error && { borderColor: 'error.main' }),
            '&:focus-within': {
              borderColor: ring,
              boxShadow: `0 0 0 1px ${ring}`,
            },
            // one block of the dark scheme: a second one would replace the
            // glass of glassSx under the same selector
            ...theme.applyStyles('dark', {
              ...darkGlass(error ? ring : theme.vars.palette.divider),
              '&:focus-within': darkGlass(ring, 2),
            }),
          };
        }}
      >
        {children}
      </Box>
      {error && (
        <Typography
          id={errorId}
          role="alert"
          variant="body2"
          sx={{ color: 'error.main', mt: 0.75 }}
        >
          {error}
        </Typography>
      )}
    </Box>
  );
}
