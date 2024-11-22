import { create } from 'zustand/index';

interface UserState {
  name?: string;
  maxCars: number;
  setName: (name: string) => void;
  setMaxCars: (max: number) => void;
}

const useUserStore = create<UserState>((set) => ({
  name: undefined,
  maxCars: 0,
  setName: (name) => set({ name: name }),
  setMaxCars: (max) => set({ maxCars: max }),
}));

export default useUserStore;
