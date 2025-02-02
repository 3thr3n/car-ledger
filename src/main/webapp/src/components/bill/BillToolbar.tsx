import { Add, Grid3x3, Home, TableChart, Upload } from '@mui/icons-material';
import { Box, Fab } from '@mui/material';
import React, { Dispatch, useState } from 'react';
import NewBillDialog from './NewBillDialog';
import { useNavigate } from '@tanstack/react-router';
import useCsvStore from '@/store/CsvStore';

export interface BillToolbarProps {
  carId: number;
  refresh: () => void;
  gridEnabled: boolean;
  setGridEnabled: Dispatch<boolean>;
}

export default function BillToolbar(props: BillToolbarProps) {
  const [open, setOpen] = useState(false);
  const navigation = useNavigate();

  const handleOpen = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const returnHome = async () => {
    await navigation({ to: '/' });
  };

  const handleSave = () => {
    handleClose();
    props.refresh();
  };

  const toggleGrid = () => {
    props.setGridEnabled(!props.gridEnabled);
  };

  const openCsvDialog = useCsvStore((state) => state.openDialog);

  const handleOpenCsvImport = () => {
    console.log('open csv import dialog');
    openCsvDialog(props.carId);
  };

  return (
    <React.Fragment>
      <Box
        sx={{
          position: 'absolute',
          top: 0,
        }}
      >
        <Fab
          variant="extended"
          size="medium"
          color="inherit"
          onClick={returnHome}
          sx={{
            mr: 1,
          }}
        >
          <Home
            sx={{
              color: 'black',
            }}
          />
        </Fab>
        <Fab
          variant="extended"
          size="medium"
          color="primary"
          onClick={handleOpen}
          sx={{
            mr: 1,
          }}
        >
          <Add sx={{ mr: 1 }} />
          New bill
        </Fab>
        <Fab
          variant="extended"
          size="medium"
          color="primary"
          onClick={handleOpenCsvImport}
          sx={{
            mr: 1,
          }}
        >
          <Upload />
        </Fab>
        <Fab
          variant="extended"
          size="medium"
          color="secondary"
          onClick={toggleGrid}
        >
          {props.gridEnabled ? <TableChart /> : <Grid3x3 />}
        </Fab>
      </Box>
      <NewBillDialog
        carId={props.carId}
        open={open}
        onClose={handleClose}
        onSave={handleSave}
      />
    </React.Fragment>
  );
}
