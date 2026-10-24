import { Autocomplete, TextField } from '@mui/material';

/** the common ones, any other can be typed */
const TESTING_BODIES = ['TÜV', 'DEKRA', 'GTÜ', 'KÜS'];

export interface TestingBodyFieldProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
}

/** who did an Abnahme or an HU, e.g. TÜV */
export default function TestingBodyField({
  label,
  value,
  onChange,
}: TestingBodyFieldProps) {
  return (
    <Autocomplete
      freeSolo
      fullWidth
      options={TESTING_BODIES}
      inputValue={value}
      onInputChange={(_, input) => onChange(input)}
      renderInput={(params) => (
        <TextField
          {...params}
          label={label}
          slotProps={{
            ...params.slotProps,
            htmlInput: { ...params.slotProps.htmlInput, maxLength: 100 },
          }}
        />
      )}
    />
  );
}
