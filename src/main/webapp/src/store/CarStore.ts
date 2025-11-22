import { create } from 'zustand';

interface CarState {
  carSize: number;
  setCarSize: (size: number) => void;
}

const useCarStore = create<CarState>((set) => ({
  carSize: 0,
  setCarSize: (size) => set({ carSize: size }),
}));

export default useCarStore;
