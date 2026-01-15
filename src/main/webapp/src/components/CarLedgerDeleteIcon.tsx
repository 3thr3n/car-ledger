import {
  Button,
  Dialog,
  DialogActions,
  DialogTitle,
  IconButton,
} from '@mui/material';
import DeleteIcon from '@mui/icons-material/Delete';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

export interface MyDeleteIconProps {
  onDelete: () => void;
}

export default function CarLedgerDeleteIcon(props: MyDeleteIconProps) {
  const { t } = useTranslation();
  const [open, setOpen] = useState(false);

  const areYouSure = () => {
    setOpen(true);
  };

  const handleClose = () => {
    setOpen(false);
  };

  const handleYes = () => {
    setOpen(false);
    props.onDelete();
  };

  return (
    <>
      <IconButton color="error" onClick={() => areYouSure()}>
        <DeleteIcon />
      </IconButton>
      <Dialog open={open} onClose={handleClose}>
        <DialogTitle>This will delete the entry, continue?</DialogTitle>
        <DialogActions>
          <Button onClick={handleClose}>{t('app.button.cancel')}</Button>
          <Button onClick={handleYes}>{t('app.button.continue')}</Button>
        </DialogActions>
      </Dialog>
    </>
  );
}
