import { TextField } from '@mui/material';
import { NumericFormat } from 'react-number-format';

export interface BillNumericInputProps {
  maxInput: number;
  label: string;
  name: string;
  required?: boolean;
  suffix?: string;
  decimalScale?: number;
}

export default function BillNumericInput(props: BillNumericInputProps) {
  return (
    <NumericFormat
      sx={{
        margin: 1,
      }}
      label={props.label}
      name={props.name}
      suffix={props.suffix}
      allowedDecimalSeparators={[',', '.']}
      customInput={TextField}
      decimalScale={props.decimalScale ?? 1}
      fixedDecimalScale
      isAllowed={(values) => {
        const { floatValue } = values;
        return (floatValue ?? 0) < props.maxInput;
      }}
      required={props.required}
    />
  );
}
