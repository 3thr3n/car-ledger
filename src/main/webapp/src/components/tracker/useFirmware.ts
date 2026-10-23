import { useQuery } from '@tanstack/react-query';
import { getFirmwareSettingsOptions } from '@/generated/@tanstack/react-query.gen';
import { localClient } from '@/utils/QueryClient';

export interface Firmware {
  /** set up on the server, else flashing is hidden */
  version?: string;
  /** Web Serial: Chrome or Edge on a desktop */
  serial: boolean;
}

/**
 * Whether a tracker can be flashed from this browser: the server has a release
 * of the firmware and the browser has Web Serial
 */
export default function useFirmware(): Firmware {
  const { data } = useQuery(
    getFirmwareSettingsOptions({ client: localClient }),
  );
  return {
    version: data?.version ?? undefined,
    serial: typeof navigator !== 'undefined' && 'serial' in navigator,
  };
}
