import { create } from 'zustand';

interface CsvState {
	open: boolean;
	carId?: number;
	closeDialog: () => void;
	openDialog: (carId: number) => void;
}

const useCsvStore = create<CsvState>((set) => ({
	open: false,
	carId: undefined,
	closeDialog: () => set({ open: false, carId: undefined }),
	openDialog: (carId: number) => set({ open: true, carId: carId })
}));

export default useCsvStore;