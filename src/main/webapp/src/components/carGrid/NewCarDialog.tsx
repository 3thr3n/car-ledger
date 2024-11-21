import { CarInputPojo } from '@/generated';
import { localClient } from '@/utils/QueryClient';
import { createCarMutation } from '@/generated/@tanstack/react-query.gen';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle, TextField } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import React, { useEffect, useRef } from 'react';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import useCarStore from '@/store/CarStore';

export interface NewCarDialogProps {
	onSave: () => void;
}

export default function NewCarDialog(props: NewCarDialogProps) {
	const open = useCarStore((store) => store.open);
	const closeDialog = useCarStore((store) => store.closeDialog);

	const { onSave } = props;

	const handleClose = () => {
		closeDialog();
	};

	const descriptionRef = useRef<HTMLDivElement>(null);

	useEffect(() => {
		if (open) {
			setTimeout(() => {
				descriptionRef.current?.focus();
				console.log(descriptionRef);
			}, 1000);
		}
	});

	const { mutate } = useMutation({
		...createCarMutation({
			client: localClient
		}),
		onSuccess: () => {
			toast.info('Car saved!');
		},
		onSettled: () => {
			onSave();
			closeDialog();
		},
		onError: (error: BackendError) => {
			if (error.status === 400) {
				toast.error('Maximum of cars reached!');
			} else {
				console.warn('Non successful response', error.status);
				toast.error('Backend failed!');
			}
		}
	});

	const handleSave = (newCar: CarInputPojo) => {
		mutate({ body: newCar });
	};

	return (
		<Dialog
			onClose={handleClose}
			open={open}
			PaperProps={{
				component: 'form',
				onSubmit: (event: React.FormEvent<HTMLFormElement>) => {
					event.preventDefault();
					const formData = new FormData(event.currentTarget);
					const formJson = Object.fromEntries(formData.entries());
					handleSave(formJson);
				}
			}}
		>
			<DialogTitle>Add new car</DialogTitle>
			<DialogContent>
				<TextField
					sx={{
						mt: 1
					}}
					ref={descriptionRef}
					autoFocus
					required
					fullWidth
					id="description"
					name="description"
					label="Description"
					variant="outlined"
				/>
			</DialogContent>
			<DialogActions>
				<Button onClick={handleClose}>Cancel</Button>
				<Button type="submit">Save</Button>
			</DialogActions>
		</Dialog>
	);
}