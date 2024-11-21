import { BillInputPojo } from '@/generated';
import { localClient } from '@/utils/QueryClient';
import { addNewBillMutation } from '@/generated/@tanstack/react-query.gen';
import { Button, Dialog, DialogActions, DialogContent, DialogTitle } from '@mui/material';
import { useMutation } from '@tanstack/react-query';
import React from 'react';
import { toast } from 'react-toastify';
import { BackendError } from '@/utils/BackendError';
import { DatePicker } from '@mui/x-date-pickers';
import dayjs from 'dayjs';
import BillNumericInput from '@/components/bill/BillNumericInput';

const MAX_NUMBER_INPUT = 10000;

export interface NewBillDialogProps {
	carId: number,
	open: boolean,
	onClose: () => void
	onSave: () => void
}

export default function NewBillDialog(props: NewBillDialogProps) {
	const { onClose, onSave, open } = props;

	const handleClose = () => {
		onClose();
	};

	const { mutate } = useMutation({
		...addNewBillMutation({
			client: localClient
		}),
		onSuccess: () => {
			toast.info('Bill saved!');
		},
		onSettled: () => {
			onSave();
		},
		onError: (error: BackendError) => {
			console.warn('Non successful response', error.status);
			toast.error('Backend failed!');
		}
	});

	const handleSave = (newBill: BillInputPojo) => {
		mutate({
			body: newBill,
			path: {
				carId: props.carId
			}
		});
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
					const formJson = Object.fromEntries(formData.entries()) as any;
					handleSave({
						...formJson,
						day: dayjs(formJson.day, 'DD.MM.YYYY').format('YYYY-MM-DD'),
						distance: Number.parseFloat(formJson.distance),
						unit: Number.parseFloat(formJson.unit),
						pricePerUnit: Number.parseFloat(formJson.pricePerUnit),
						estimate: Number.parseFloat(formJson.estimate)
					});
				}
			}}
		>
			<DialogTitle>Add new bill</DialogTitle>
			<DialogContent
				sx={{
					display: 'flex',
					flexDirection: 'column'
				}}
			>
				<DatePicker
					sx={{
						margin: 1
					}}
					defaultValue={dayjs()}
					label="day"
					name="day"
					disableFuture
				/>
				<BillNumericInput maxInput={MAX_NUMBER_INPUT} label="Unit" name="unit" required suffix=" L" decimalScale={2} />
				<BillNumericInput
					maxInput={MAX_NUMBER_INPUT}
					label="Price per unit"
					name="pricePerUnit"
					required
					suffix=" ct"
				/>
				<BillNumericInput maxInput={MAX_NUMBER_INPUT} label="Distance" name="distance" suffix=" km" />
				<BillNumericInput maxInput={MAX_NUMBER_INPUT} label="Estimate" name="estimate" suffix=" L" />
			</DialogContent>
			<DialogActions>
				<Button onClick={handleClose}>Cancel</Button>
				<Button type="submit">Save</Button>
			</DialogActions>
		</Dialog>
	);
}