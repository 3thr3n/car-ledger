import { Box, Card, Skeleton, Stack, Typography } from '@mui/material';
import { ReactNode } from 'react';
import { BillPaletteKey, categoryInk } from '@/utils/billTypes';

export interface StatTileProps {
  label: ReactNode;
  value: ReactNode;
  hint?: ReactNode;
  icon?: ReactNode;
  color?: BillPaletteKey | 'primary';
  loading?: boolean;
  /** larger value, used for the headline numbers */
  emphasis?: boolean;
}

export default function StatTile({
  label,
  value,
  hint,
  icon,
  color = 'primary',
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
          mb: 1.25,
        }}
      >
        {icon && (
          <Box
            sx={(theme) => ({
              display: 'grid',
              placeItems: 'center',
              width: 28,
              height: 28,
              borderRadius: 2,
              ...categoryInk(theme, color),
              backgroundColor: `rgba(${theme.vars.palette[color].mainChannel} / 0.12)`,
              '& svg': { fontSize: 17 },
            })}
          >
            {icon}
          </Box>
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
        variant={emphasis ? 'h4' : 'h5'}
        component="div"
        sx={{ fontWeight: 700 }}
      >
        {loading ? <Skeleton width="60%" /> : value}
      </Typography>
      {hint && (
        <Typography
          variant="caption"
          component="div"
          sx={{
            color: 'text.secondary',
          }}
        >
          {loading ? <Skeleton width="40%" /> : hint}
        </Typography>
      )}
    </Card>
  );
}
