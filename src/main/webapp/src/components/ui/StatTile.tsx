import { Box, Card, Skeleton, Stack, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { BillPaletteKey } from '@/utils/billTypes';
import { displayFont } from '@/theme';

export interface StatTileProps {
  label: ReactNode;
  value: ReactNode;
  hint?: ReactNode;
  /** the kind of entries counted, shown as a stripe next to the label */
  color?: BillPaletteKey;
  loading?: boolean;
  /** larger value, used for the headline numbers */
  emphasis?: boolean;
}

export default function StatTile({
  label,
  value,
  hint,
  color,
  loading,
  emphasis,
}: StatTileProps) {
  return (
    <Card sx={{ p: 2.25, height: '100%' }}>
      <Stack
        direction="row"
        spacing={1}
        sx={{
          alignItems: 'center',
          mb: 0.75,
        }}
      >
        {color && (
          <Box
            aria-hidden
            sx={{
              width: 3,
              height: 14,
              flexShrink: 0,
              borderRadius: 1,
              bgcolor: `${color}.main`,
            }}
          />
        )}
        <Typography
          variant="body2"
          noWrap
          sx={{
            color: 'text.secondary',
          }}
        >
          {label}
        </Typography>
      </Stack>
      <Typography
        className="tabular"
        component="div"
        sx={{
          fontFamily: displayFont,
          fontWeight: 650,
          fontSize: emphasis ? '2rem' : '1.625rem',
          lineHeight: 1.15,
        }}
      >
        {loading ? <Skeleton width="60%" /> : value}
      </Typography>
      {hint && (
        <Typography
          variant="caption"
          component="div"
          sx={{
            color: 'text.secondary',
            mt: 0.5,
          }}
        >
          {loading ? <Skeleton width="40%" /> : hint}
        </Typography>
      )}
    </Card>
  );
}
