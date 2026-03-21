import { create } from 'zustand';

interface CsvState {
  open: boolean;
  carId?: number;
  csvImportedAt: number | null;
  markImported: () => void;
}

const useCsvStore = create<CsvState>((set) => ({
  open: false,
  carId: undefined,
  csvImportedAt: null,
  markImported: () => set({ csvImportedAt: Date.now() }),
}));

export default useCsvStore;
