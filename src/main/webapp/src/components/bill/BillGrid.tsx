import { BillPojo } from '@/generated';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Grid2,
  Typography,
} from '@mui/material';
import React from 'react';

export interface BillGridProps {
  bill: BillPojo;
}

export default function BillGrid(props: BillGridProps) {
  const BillCardItem = (props: { name: string; value: string }) => {
    return (
      <Box mb={1} width={110}>
        <Typography variant="body2">{props.name}</Typography>
        <Typography variant="body1">{props.value}</Typography>
      </Box>
    );
  };

  const BillCardRow = (props: { children: React.ReactNode }) => {
    return (
      <Box display={'flex'} justifyContent={'center'}>
        {props.children}
      </Box>
    );
  };

  return (
    <Grid2
      size={2}
      sx={{
        width: '300px',
      }}
    >
      <Card
        variant="elevation"
        sx={{
          width: '100%',
        }}
      >
        <CardHeader title={props.bill.day} />
        <CardContent>
          <BillCardRow>
            <BillCardItem
              name={'Distance'}
              value={`${props.bill.distance} km`}
            />
            <BillCardItem
              name={'Costs'}
              value={`${props.bill.calculatedPrice} €`}
            />
          </BillCardRow>
          <BillCardRow>
            <BillCardItem name={'Unit'} value={`${props.bill.unit} l`} />
            <BillCardItem
              name={'PPU'}
              value={`${props.bill.pricePerUnit} ct`}
            />
          </BillCardRow>
          <BillCardRow>
            <BillCardItem
              name={'Actual'}
              value={`${props.bill.calculated} l/100km`}
            />
            <BillCardItem
              name={'Estimate'}
              value={`${props.bill.estimate} l/100km`}
            />
          </BillCardRow>
        </CardContent>
      </Card>
    </Grid2>
  );
}
