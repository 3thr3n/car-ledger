import { Box, InputBase, Typography } from '@mui/material';
import { ReactNode, useId } from 'react';
import { useTranslation } from 'react-i18next';
import { NumericFormat } from 'react-number-format';
import { CLUSTER_AMBER, displayFont } from '@/theme';

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
        display: 'grid',
        gridTemplateColumns: 'minmax(0, auto) minmax(0, 1fr)',
        alignItems: 'center',
        columnGap: 2,
        py: 1,
        '& + &': { borderTop: 1, borderColor: 'divider' },
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
            p: 0,
            textAlign: 'right',
            fontFamily: displayFont,
            fontWeight: 700,
            fontSize: { xs: '1.625rem', sm: '1.875rem' },
            lineHeight: 1.2,
            fontVariantNumeric: 'tabular-nums',
            ...theme.applyStyles('dark', { color: CLUSTER_AMBER }),
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
        sx={(theme) => ({
          px: 2,
          py: 0.5,
          borderRadius: '10px',
          border: 1,
          borderColor: error ? 'error.main' : 'divider',
          '&:focus-within': {
            borderColor: error ? 'error.main' : 'primary.main',
            boxShadow: `0 0 0 1px ${error ? theme.vars.palette.error.main : theme.vars.palette.primary.main}`,
          },
          // the glass of the pump, sunk into the panel
          ...theme.applyStyles('dark', {
            bgcolor: '#0a1018',
            borderColor: error ? 'error.main' : 'transparent',
            boxShadow: `inset 0 2px 10px rgba(0 0 0 / 0.6), 0 0 0 1px ${error ? theme.vars.palette.error.main : theme.vars.palette.divider}`,
            '&:focus-within': {
              boxShadow: `inset 0 2px 10px rgba(0 0 0 / 0.6), 0 0 0 2px ${error ? theme.vars.palette.error.main : theme.vars.palette.primary.main}`,
            },
          }),
        })}
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
