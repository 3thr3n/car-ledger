import { Typography, TypographyVariant } from '@mui/material';

export default function CarLedgerSubPageHeader({
  title,
  isCardHeader,
}: {
  title: string;
  isCardHeader?: boolean;
}) {
  let variant: TypographyVariant = 'h4';

  if (isCardHeader) {
    variant = 'h6';
  }

  return (
    <Typography variant={variant} gutterBottom>
      {title}
    </Typography>
  );
}
