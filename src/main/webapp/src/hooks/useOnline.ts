import { onlineManager } from '@tanstack/react-query';
import { useSyncExternalStore } from 'react';

/** whether the queries run, the same as TanStack Query sees it */
export default function useOnline() {
  return useSyncExternalStore(
    (listener) => onlineManager.subscribe(listener),
    () => onlineManager.isOnline(),
  );
}
