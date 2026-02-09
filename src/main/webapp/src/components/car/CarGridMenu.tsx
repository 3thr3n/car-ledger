import { Theme } from '@emotion/react';
import { MoreVert, Upload } from '@mui/icons-material';
import { IconButton, Menu, MenuItem, SxProps } from '@mui/material';
import React, { MouseEvent, useState } from 'react';

export interface CarGridMenuProps {
  sx: SxProps<Theme>;
  id: number;
}

export default function CarGridMenu(props: CarGridMenuProps) {
  const [anchorEl, setAnchorEl] = useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

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
          <Upload sx={{ mr: 1 }} />
          Import CSV
        </MenuItem>
      </Menu>
    </React.Fragment>
  );
}
