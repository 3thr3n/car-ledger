import { create } from 'zustand';

interface CarState {
  carSize: number;
  setCarSize: (size: number) => void;

  open: boolean;
  closeDialog: () => void;
  openDialog: () => void;
}

const useCarStore = create<CarState>((set) => ({
  carSize: 0,
  setCarSize: (size) => set({ carSize: size }),

  open: false,
  closeDialog: () => set({ open: false }),
  openDialog: () => set({ open: true }),
}));

export default useCarStore;
