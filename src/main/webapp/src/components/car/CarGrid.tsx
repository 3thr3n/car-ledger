import { Card, CardContent, SxProps, Theme } from '@mui/material';
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
    <Card
      onClick={props.click}
      variant="elevation"
      sx={{
        ...props.sx,
        height: props.height,
        width: '100%',
        cursor: props.click ? 'pointer' : 'default',
        border: '1px solid #2a2a2a',
        transition: '0.2s ease',
        ':hover': {
          borderColor: (theme) => theme.palette.primary.main,
          transform: 'translateY(-3px)',
        },
      }}
    >
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
    </Card>
  );
}
