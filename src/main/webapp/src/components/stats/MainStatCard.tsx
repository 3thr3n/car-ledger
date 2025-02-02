import { Box, Typography } from '@mui/material';
import BaseStatCard from './BaseStatCard';
import React from 'react';

export interface MainStatCardProps {
  title: string;
  postfix: string;
  total: number | undefined;
}

export default function MainStatCard(props: MainStatCardProps) {
  return (
    <BaseStatCard>
      <Typography width="100%" display="flex" justifyContent="center">
        {props.title}
      </Typography>
      <Box justifyContent="center" display="flex">
        <Typography
          component="span"
          variant="h4"
          display="flex"
          flexDirection="row"
          alignItems="center"
        >
          {props.total}
          <Typography alignSelf="end">&nbsp;{props.postfix}</Typography>
        </Typography>
      </Box>
    </BaseStatCard>
  );
}
