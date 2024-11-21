import { Add } from '@mui/icons-material';
import { Fab } from '@mui/material';
import React from 'react';
import NewCarDialog from './NewCarDialog';
import useCarStore from '@/store/CarStore';

export interface NewCarProps {
	refetch: () => void;
}

export default function NewCar(props: NewCarProps) {
	const openDialog = useCarStore((store) => store.openDialog);

	const handleOpen = () => {
		openDialog();
	};

	const handleSave = () => {
		props.refetch();
	};

	return (
		<React.Fragment>
			<Fab
				variant="extended"
				size="medium"
				color="primary"
				onClick={handleOpen}
				sx={{
					position: 'absolute',
					top: 0
				}}
			>
				<Add sx={{ mr: 1 }} />
				New car
			</Fab>

			<NewCarDialog
				onSave={handleSave}
			/>
		</React.Fragment>
	);
}