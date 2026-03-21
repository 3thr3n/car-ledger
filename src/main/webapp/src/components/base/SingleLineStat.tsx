import { Box, Typography } from '@mui/material';

export default function SingleLineStat({
  label,
  value,
  type,
}: {
  label: string;
  value?: string | number;
  type?: string;
}) {
  return (
    <Typography variant="body1" component="div" display="flex">
      <strong>{label}</strong> <Box flexGrow={1} />
      {value ?? '—'}
      {type ? ' ' + type : ''}
    </Typography>
  );
}
