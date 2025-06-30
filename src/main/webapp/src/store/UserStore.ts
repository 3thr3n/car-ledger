import { create } from 'zustand';

interface UserState {
  loggedIn: boolean;
  name?: string;
  maxCars: number;
  setName: (name: string) => void;
  setMaxCars: (max: number) => void;
  setLoggedIn: (loggedIn: boolean) => void;
}

const useUserStore = create<UserState>((set) => ({
  loggedIn: false,
  name: undefined,
  maxCars: 0,
  setName: (name) => set({ name: name }),
  setMaxCars: (max) => set({ maxCars: max }),
  setLoggedIn: (loggedIn) => set({ loggedIn }),
}));

export default useUserStore;
