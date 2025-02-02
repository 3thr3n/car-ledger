import React, { createRef, useState } from 'react';
import BillToolbar from './BillToolbar';
import BillGridList, { BillGridRef } from './BillGridList';
import BillTable, { BillTableRef } from '@/components/bill/BillTable';
import { useMutation } from '@tanstack/react-query';
import { deleteBillMutation } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';
import { toast } from 'react-toastify';
import { MinimalStatistics } from '../stats/MinimalStatistics';

export interface BillParams {
  carId: number;
}

export default function Bill(props: BillParams) {
  const [grid, setGrid] = useState(false);
  const [expanded, setExpanded] = useState(false);

  const table = createRef<BillTableRef>();
  const billGrid = createRef<BillGridRef>();

  const refresh = () => {
    if (grid) {
      billGrid.current?.refresh();
    } else {
      table.current?.refresh();
    }
  };

  const updateGrid = (state: boolean) => {
    setExpanded(false);
    setGrid(state);
  };

  const { mutate } = useMutation({
    ...deleteBillMutation({
      client: localClient,
    }),
    onSuccess: () => {
      toast.info('Bill deleted!');
    },
    onSettled: () => {
      refresh();
    },
    onError: () => {
      toast.error('Backend failed!');
    },
  });

  const deleteBill = (id: string | number) => {
    // TODO: Popup if you are sure
    mutate({
      path: {
        carId: props.carId,
        billId: id as number,
      },
    });
  };

  const renderTableOrGrid = () => {
    if (grid) {
      return <BillGridList ref={billGrid} carId={props.carId} />;
    } else {
      return (
        <BillTable
          ref={table}
          carId={props.carId}
          delete={(id) => deleteBill(id)}
        />
      );
    }
  };

  return (
    <React.Fragment>
      <BillToolbar
        carId={props.carId}
        refresh={refresh}
        gridEnabled={grid}
        setGridEnabled={updateGrid}
      />
      <MinimalStatistics
        carId={props.carId}
        expandend={expanded}
        setExpanded={setExpanded}
      />
      {renderTableOrGrid()}
    </React.Fragment>
  );
}
