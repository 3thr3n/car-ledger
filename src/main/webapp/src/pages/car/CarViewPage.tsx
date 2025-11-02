import { NavigateOptions } from '@tanstack/router-core';

export interface CarViewPageProperties {
  id: string;
  navigate: (path: NavigateOptions) => void;
}

export default function CarViewPage({ navigate }: CarViewPageProperties) {
  return <></>;
}
