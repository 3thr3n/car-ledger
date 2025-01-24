import { Add } from '@mui/icons-material';
import { Fab, Tooltip } from '@mui/material';
import React, { useEffect, useState } from 'react';
import NewCarDialog from './NewCarDialog';
import useCarStore from '@/store/CarStore';
import useUserStore from '@/store/UserStore';

export interface NewCarProps {
  refetch: () => void;
}

export default function NewCar(props: NewCarProps) {
  const [disabled, setDisabled] = useState(true);

  const openDialog = useCarStore((store) => store.openDialog);
  const maxCars = useUserStore((state) => state.maxCars);
  const currentCarSize = useCarStore((state) => state.carSize);

  const handleOpen = () => {
    openDialog();
  };

  const handleSave = () => {
    props.refetch();
  };

  useEffect(() => {
    if (maxCars <= currentCarSize) {
      setDisabled(true);
    } else {
      setDisabled(false);
    }
  }, [maxCars, currentCarSize]);

  let tooltip =
    'Add new Car, you can still add ' + (maxCars - currentCarSize) + ' cars';
  if (disabled) {
    tooltip =
      'Max cars reached, either delete one or ask the administrator to allow one more!';
  }

  return (
    <React.Fragment>
      <Tooltip title={tooltip}>
        <span>
          <Fab
            disabled={disabled}
            variant="extended"
            size="medium"
            color="primary"
            onClick={handleOpen}
            sx={{
              position: 'absolute',
              top: 0,
              pointerEvents: 'all !important'
            }}
          >
            <Add sx={{ mr: 1 }} />
            New car
          </Fab>
        </span>
      </Tooltip>

      <NewCarDialog onSave={handleSave} />
    </React.Fragment>
  );
}
