import { Card, CardContent, Grid2 } from '@mui/material';
import React from 'react';

export interface BaseStatCardProps {
  children: React.ReactNode;
  minWidth?: string;
  maxWidth?: string;
}

export default function BaseStatCard(props: BaseStatCardProps) {
  return (
    <Grid2 size={3}>
      <Card
        sx={{
          maxWidth: props.maxWidth ?? '350px',
          minWidth: props.minWidth ?? '200px',
        }}
      >
        <CardContent>{props.children}</CardContent>
      </Card>
    </Grid2>
  );
}
