import { HiLo } from '@/generated';
import { Box, Typography } from '@mui/material';
import { KeyboardArrowDown, KeyboardArrowUp } from '@mui/icons-material';
import BaseStatCard from './BaseStatCard';

export interface HighLowCardProps {
  title: string;
  postfix: string;
  highLow: HiLo | undefined;
  lowIsGood?: boolean;
}

export default function HighLowCard(props: HighLowCardProps) {
  return (
    <BaseStatCard>
      <Typography width="100%" display="flex" justifyContent="center">
        {props.title}
      </Typography>
      <Box justifyContent="center" display="flex" flexWrap="wrap">
        <Typography
          component="span"
          variant="h4"
          display="flex"
          alignItems="center"
        >
          <KeyboardArrowUp
            fontSize="large"
            color={props.lowIsGood ? 'error' : 'success'}
          />{' '}
          {props.highLow?.max}
          <Typography alignSelf="end">&nbsp;{props.postfix}</Typography>
        </Typography>
        <Typography
          component="span"
          variant="h4"
          display="flex"
          alignItems="center"
        >
          <KeyboardArrowDown
            fontSize="large"
            color={props.lowIsGood ? 'success' : 'error'}
          />{' '}
          {props.highLow?.min}
          <Typography alignSelf="end">&nbsp;{props.postfix}</Typography>
        </Typography>
      </Box>
    </BaseStatCard>
  );
}
