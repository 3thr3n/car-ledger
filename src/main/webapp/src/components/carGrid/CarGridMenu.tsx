import { Theme } from '@emotion/react';
import { MoreVert } from '@mui/icons-material';
import { IconButton, Menu, MenuItem, SxProps } from '@mui/material';
import React, { MouseEvent, useState } from 'react';
import useCsvStore from '@/store/CsvStore';

export interface CarGridMenuProps {
  sx: SxProps<Theme>;
  id: number;
}

export default function CarGridMenu(props: CarGridMenuProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const openCsvDialog = useCsvStore((state) => state.openDialog);

  function openMenu(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation();
    setAnchorEl(event.currentTarget);
  }

  function handleClose(event: MouseEvent<HTMLDivElement>) {
    event.stopPropagation();
    setAnchorEl(null);
  }

  function handleCsvImport(event: MouseEvent<HTMLLIElement>) {
    event.stopPropagation();
    setAnchorEl(null);
    openCsvDialog(props.id);
  }

  return (
    <React.Fragment>
      <IconButton
        sx={{
          ...props.sx,
          position: 'absolute',
          top: 10,
          right: 10,
        }}
        onClick={openMenu}
      >
        <MoreVert />
      </IconButton>
      <Menu anchorEl={anchorEl} open={open} onClose={handleClose}>
        <MenuItem onClick={handleCsvImport} key="import">
          Import CSV
        </MenuItem>
      </Menu>
    </React.Fragment>
  );
}
