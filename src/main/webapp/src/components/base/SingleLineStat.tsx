import { Typography } from '@mui/material';

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
    <Typography variant="body1">
      <strong>{label}</strong> {value ?? '—'}{' '}
      {type ? <Typography variant="caption">{type}</Typography> : ''}
    </Typography>
  );
}
