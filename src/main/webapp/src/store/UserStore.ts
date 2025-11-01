import { create } from 'zustand';

interface UserState {
  name?: string;
  loggedIn: boolean;
  maxCars: number;
  setName: (name: string) => void;
  setMaxCars: (max: number) => void;
  setLoggedIn: (loggedIn: boolean) => void;
}

const useUserStore = create<UserState>((set) => ({
  name: undefined,
  loggedIn: false,
  maxCars: 0,
  setName: (name) => set({ name: name }),
  setMaxCars: (max) => set({ maxCars: max }),
  setLoggedIn: (loggedIn) => set({ loggedIn }),
}));

export default useUserStore;
