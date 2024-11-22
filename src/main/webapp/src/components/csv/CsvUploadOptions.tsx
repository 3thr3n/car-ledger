import { MenuItem, Select, SelectChangeEvent } from '@mui/material';
import { useState } from 'react';

export interface CsvUploadOptionsProps {
  headers: string[];
  defaultColumn?: number;
  onOptionChanged: (value: number) => void;
}

export default function CsvUploadOptions(props: CsvUploadOptionsProps) {
  const [header, setHeader] = useState(props.defaultColumn ?? 0);

  const handleChange = (event: SelectChangeEvent<number>) => {
    const newHeaderIndex = event.target.value as number;
    setHeader(newHeaderIndex);
    props.onOptionChanged(newHeaderIndex);
  };

  const items = props.headers.map((head, index) => (
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
