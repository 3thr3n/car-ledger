import { create } from 'zustand';

interface CsvState {
  open: boolean;
  carId?: number;
  csvImportedAt: number | null;
  closeDialog: () => void;
  openDialog: (carId: number) => void;
  markImported: () => void;
}

const useCsvStore = create<CsvState>((set) => ({
  open: false,
  carId: undefined,
  csvImportedAt: null,
  closeDialog: () => set({ open: false, carId: undefined }),
  openDialog: (carId: number) => set({ open: true, carId: carId }),
  markImported: () => set({ csvImportedAt: Date.now() }),
}));

export default useCsvStore;
