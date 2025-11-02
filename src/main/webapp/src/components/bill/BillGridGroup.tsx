import { BillPojo } from '@/generated';
import { Card, CardContent, CardHeader, Grid } from '@mui/material';
import React from 'react';
import BillGrid from './BillGrid';

export interface BillGridGroupProps {
  year: string;
  bills: BillPojo[];
}

export default function BillGridGroup(props: BillGridGroupProps) {
  function renderComponent() {
    return props.bills.map((bill) => {
      return <BillGrid bill={bill} key={bill.id} />;
    });
  }

  return (
    <Grid
      container
      sx={{
        overflow: 'auto',
        width: '100%',
      }}
    >
      <Card
        className="background-card"
        variant="elevation"
        sx={{
          width: '100%',
        }}
      >
        <CardHeader title={props.year} />
        <CardContent>
          <Grid
            container
            spacing={2}
            columns={{ xl: 14, md: 8, sm: 6, xs: 4 }}
            sx={{
              overflow: 'auto',
            }}
          >
            {renderComponent()}
          </Grid>
        </CardContent>
      </Card>
    </Grid>
  );
}
