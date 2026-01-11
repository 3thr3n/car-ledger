import { Button as MuiButton, ButtonProps } from '@mui/material';
import { ReactNode } from 'react';

export interface CarLedgerButtonProps {
  onClick?: () => void;
  disabled?: boolean;
  children?: ReactNode;
  variant?: 'outlined' | 'contained';
  color?: 'secondary' | 'primary';
  href?: string;
  fullWidth?: boolean;
}

export default function CarLedgerButton(props: CarLedgerButtonProps) {
  const muiProps: ButtonProps = {
    onClick: props.onClick,
    disabled: props.disabled,
    children: props.children,
    href: props.href,
    fullWidth: props.fullWidth,
    variant: props.variant ?? 'outlined',
    color: props.color ?? 'primary',
  };

  if (props.href) {
    muiProps.variant = 'outlined';
    muiProps.color = 'primary';
  }

  return <MuiButton {...muiProps} />;
}
