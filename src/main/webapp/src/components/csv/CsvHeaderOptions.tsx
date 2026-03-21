import { MenuItem, Select, SelectChangeEvent } from '@mui/material';
import { useState } from 'react';

export interface CsvHeaderOptionsProps {
  headers: string[];
  defaultColumn?: number;
  onOptionChanged: (value: number | undefined) => void;
}

export default function CsvHeaderOptions(props: CsvHeaderOptionsProps) {
  const [header, setHeader] = useState(props.defaultColumn ?? 0);

  const handleChange = (event: SelectChangeEvent<number>) => {
    const newHeaderIndex = event.target.value as number;
    setHeader(newHeaderIndex);
    props.onOptionChanged(newHeaderIndex == 0 ? undefined : newHeaderIndex - 1);
  };

  const headers = ['N/A', ...props.headers];

  const items = headers.map((head, index) => (
    <MenuItem key={index} value={index}>
      {head}
    </MenuItem>
  ));

  return (
    <Select
      variant={'standard'}
      value={header}
      onChange={handleChange}
      sx={{
        ml: 2,
        width: 200,
      }}
    >
      {items}
    </Select>
  );
}
