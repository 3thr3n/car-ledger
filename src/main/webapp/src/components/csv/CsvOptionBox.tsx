import { Box, Typography } from '@mui/material';
import CsvHeaderOptions from '@/components/csv/CsvHeaderOptions';

export function CsvOptionBox(props: {
  headers: string[];
  title: string;
  mandatory?: boolean;
  defaultColumn?: number;
  onOptionChanged: (value: number | undefined) => void;
}) {
  return (
    <Box display="flex" alignItems="center" mb={1}>
      <Typography variant="body1" minWidth={120} maxWidth={250}>
        {props.title} {props.mandatory && '*'}
      </Typography>
      <CsvHeaderOptions
        headers={props.headers}
        defaultColumn={props.defaultColumn}
        onOptionChanged={props.onOptionChanged}
      />
    </Box>
  );
}
