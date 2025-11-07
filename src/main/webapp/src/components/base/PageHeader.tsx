import { Typography, TypographyVariant } from '@mui/material';

export default function PageHeader({
  title,
  isMobile,
  isCardHeader,
}: {
  title: string;
  isMobile?: boolean;
  isCardHeader?: boolean;
}) {
  let variant: TypographyVariant = 'h4';

  if (isMobile && isCardHeader) {
    variant = 'subtitle1';
  } else if (isMobile || isCardHeader) {
    variant = 'h6';
  }

  return (
    <Typography variant={variant} gutterBottom>
      {title}
    </Typography>
  );
}
