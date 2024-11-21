import { create } from 'zustand';

interface CarState {
	open: boolean;
	closeDialog: () => void;
	openDialog: () => void;
}

const useCarStore = create<CarState>((set) => ({
	open: false,
	closeDialog: () => set({ open: false }),
	openDialog: () => set({ open: true })
}));

export default useCarStore;