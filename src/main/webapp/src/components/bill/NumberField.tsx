import { TextField } from '@mui/material';
import { NumericFormat } from 'react-number-format';
import { ReactNode } from 'react';
import { useTranslation } from 'react-i18next';

export interface NumberFieldProps {
  label: ReactNode;
  value?: number;
  onChange: (value: number | undefined) => void;
  decimalScale?: number;
  suffix?: string;
  /** shown in red, replaces the helper text */
  error?: string;
  helperText?: ReactNode;
  required?: boolean;
  disabled?: boolean;
  placeholder?: string;
  thousandSeparator?: boolean;
  max?: number;
}

/**
 * Number input that follows the separators of the selected language
 */
export default function NumberField({
  label,
  value,
  onChange,
  decimalScale = 2,
  suffix,
  error,
  helperText,
  required,
  disabled,
  placeholder,
  thousandSeparator = true,
  max,
}: NumberFieldProps) {
  const { i18n } = useTranslation();
  const german = i18n.language === 'DE';

  return (
    <NumericFormat
      customInput={TextField}
      label={label}
      value={value ?? ''}
      onValueChange={(values) => onChange(values.floatValue)}
      decimalScale={decimalScale}
      decimalSeparator={german ? ',' : '.'}
      thousandSeparator={thousandSeparator ? (german ? '.' : ',') : undefined}
      allowedDecimalSeparators={[',', '.']}
      allowNegative={false}
      suffix={suffix}
      isAllowed={({ floatValue }) =>
        max === undefined || (floatValue ?? 0) <= max
      }
      onFocus={(event) => event.target.select()}
      error={!!error}
      helperText={error ?? helperText}
      required={required}
      disabled={disabled}
      placeholder={placeholder}
      inputMode="decimal"
    />
  );
}
