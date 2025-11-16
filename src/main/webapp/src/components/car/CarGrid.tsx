import { Card, CardContent, Grid, SxProps, Theme } from '@mui/material';
import React, { MouseEventHandler } from 'react';

export function CarGrid(props: {
  size?: number;
  height?: number;
  children: React.ReactNode;
  sx?: SxProps<Theme>;
  click?: MouseEventHandler<HTMLDivElement>;
  index?: number;
}) {
  return (
    <Grid height={props.height} justifyContent="center" display="flex">
      <Card
        onClick={props.click}
        variant="elevation"
        sx={{
          ...props.sx,
          height: props.height,
          width: '100%',
          cursor: props.click ? 'pointer' : 'default',
        }}
      >
        {props.children}
      </Card>
    </Grid>
  );
}

export function CarGridContent(props: { children: React.ReactNode }) {
  return (
    <CardContent
      sx={{
        alignItems: 'center',
        justifyContent: 'center',
        display: 'flex',
        flexDirection: 'column',
        position: 'relative',
        height: '100%',
        width: '100%',
      }}
    >
      {props.children}
    </CardContent>
  );
}
