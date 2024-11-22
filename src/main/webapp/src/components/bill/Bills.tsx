import React, { createRef, useState } from 'react';
import BillToolbar from './BillToolbar';
import BillGridList, { BillGridRef } from './BillGridList';
import BillTable, { BillTableRef } from '@/components/bill/BillTable';

export interface BillParams {
  carId: number;
}

export default function Bill(props: BillParams) {
  const [grid, setGrid] = useState(false);

  const table = createRef<BillTableRef>();
  const billGrid = createRef<BillGridRef>();

  const refresh = () => {
    if (grid) {
      billGrid.current?.refresh();
    } else {
      table.current?.refresh();
    }
  };

  const renderTableOrGrid = () => {
    if (grid) {
      return <BillGridList ref={billGrid} carId={props.carId} />;
    } else {
      return <BillTable ref={table} carId={props.carId} />;
    }
  };

  return (
    <React.Fragment>
      <BillToolbar
        carId={props.carId}
        refresh={refresh}
        gridEnabled={grid}
        setGridEnabled={setGrid}
      />
      {renderTableOrGrid()}
    </React.Fragment>
  );
}
