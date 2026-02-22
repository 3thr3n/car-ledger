import { Box, BoxProps } from '@mui/material';
import { ReactNode, Ref } from 'react';

export interface CarLedgerPageProps {
  id: string;
  children: ReactNode;
  py?: number;
  ref?: Ref<HTMLDivElement>;
}

export default function CarLedgerPage(props: CarLedgerPageProps) {
  const myProps: BoxProps = {
    py: props.py ?? 3,
    overflow: 'scroll',
    children: props.children,
    id: props.id,
    ref: props.ref,
    width: '100%',
  };

  return <Box {...myProps} />;
}
