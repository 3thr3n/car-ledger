import { Button, Stack } from '@mui/material';
import AttachFileRounded from '@mui/icons-material/AttachFileRounded';
import ExpandMoreRounded from '@mui/icons-material/ExpandMoreRounded';
import { ReactNode } from 'react';

/** opens the details of an entry in a list, with the count of its files */
export default function DetailsButton({
  expanded,
  onClick,
  documents,
  children,
}: {
  expanded: boolean;
  onClick: () => void;
  documents: number;
  children: ReactNode;
}) {
  return (
    <Button
      size="small"
      color="inherit"
      onClick={onClick}
      aria-expanded={expanded}
      endIcon={
        <ExpandMoreRounded
          sx={{
            transform: expanded ? 'rotate(180deg)' : undefined,
            transition: 'transform 150ms',
          }}
        />
      }
      sx={{ mt: 0.5, ml: -0.75, color: 'text.secondary' }}
    >
      {children}
      {documents > 0 && (
        <Stack
          component="span"
          direction="row"
          spacing={0.25}
          sx={{
            alignItems: 'center',
            ml: 1,
            '& svg': { fontSize: 15 },
          }}
        >
          <AttachFileRounded />
          <span>{documents}</span>
        </Stack>
      )}
    </Button>
  );
}
