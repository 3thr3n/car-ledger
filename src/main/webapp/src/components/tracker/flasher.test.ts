import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { FirmwareManifest } from '@/generated';

/** what the mocked bootloader of the tracker was asked */
const chip = vi.hoisted(() => ({
  written: [] as { address: number; data: Uint8Array }[],
  /** the partition table on the tracker */
  table: undefined as Uint8Array | undefined,
  config: new Uint8Array(0),
  resets: 0,
}));

vi.mock('esptool-js', () => ({
  Transport: class {
    disconnect = vi.fn(async () => {});
    rawRead = vi.fn(async () => {});
  },
  ESPLoader: class {
    chip = { CHIP_NAME: 'ESP32' };
    main = vi.fn(async () => {});
    after = vi.fn(async () => {
      chip.resets++;
    });
    readFlash = vi.fn(async (address: number, size: number) => {
      if (address === 0x8000) {
        if (!chip.table) throw new Error('read failed');
        return chip.table.slice(0, size);
      }
      return chip.config.slice(0, size);
    });
    writeFlash = vi.fn(
      async (options: {
        fileArray: { address: number; data: Uint8Array }[];
      }) => {
        chip.written.push(...options.fileArray);
      },
    );
  },
}));

const { flashTracker, parseSettings, readSettings } = await import('./flasher');

const TABLE = new Uint8Array([0xaa, 0x50, 1, 2, 3]);
const MANIFEST = {
  name: 'CarLedger GPS tracker',
  version: 'v1.1.0',
  spec: 1,
  layout: 1,
  capabilities: ['install', 'update-firmware', 'settings', 'read-settings'],
  builds: [
    {
      chipFamily: 'ESP32',
      parts: [
        { path: 'partitions.bin', offset: 0x8000 },
        { path: 'firmware.bin', offset: 0x10000 },
      ],
    },
  ],
  config: {
    offset: 0x3f0000,
    size: 8192,
    maxLength: 8192,
    format: 'json-nul',
    version: 1,
    schema: {},
  },
} as FirmwareManifest;

const port = {} as SerialPort;
const files: Record<string, Uint8Array> = {
  'partitions.bin': TABLE,
  'firmware.bin': new Uint8Array([9, 9, 9]),
};

const flash = (
  mode: 'install' | 'settings' | 'firmware',
  confirmLayout = vi.fn(async () => true),
) =>
  flashTracker(port, {
    manifest: MANIFEST,
    mode,
    config: mode === 'firmware' ? undefined : new Uint8Array([0x7b, 0x7d, 0]),
    download: async (path) => files[path],
    confirmLayout,
    onStep: () => {},
    onProgress: () => {},
    onOutput: () => {},
  });

const addresses = () => chip.written.map((file) => file.address);

beforeEach(() => {
  chip.written = [];
  chip.table = TABLE;
  chip.config = new Uint8Array(0);
  chip.resets = 0;
});

describe('flashTracker', () => {
  it('installs the firmware and the settings', async () => {
    await flash('install');
    expect(addresses()).toEqual([0x8000, 0x10000, 0x3f0000]);
  });

  it('writes only the settings', async () => {
    await flash('settings');
    expect(addresses()).toEqual([0x3f0000]);
  });

  it('updates only the firmware and never writes the config partition', async () => {
    const confirm = vi.fn(async () => true);
    await flash('firmware', confirm);

    expect(addresses()).toEqual([0x8000, 0x10000]);
    // the partitions on the tracker are the ones of the release
    expect(confirm).not.toHaveBeenCalled();
  });

  it('asks before the firmware only with other partitions', async () => {
    chip.table = new Uint8Array([0xaa, 0x50, 9, 9, 9]);
    const confirm = vi.fn(async () => true);
    await flash('firmware', confirm);

    expect(confirm).toHaveBeenCalledWith('different');
    expect(addresses()).toEqual([0x8000, 0x10000]);
  });

  it('writes nothing when the other partitions are not confirmed', async () => {
    chip.table = undefined;
    const confirm = vi.fn(async () => false);

    await expect(flash('firmware', confirm)).rejects.toMatchObject({
      code: 'cancelled',
    });
    expect(confirm).toHaveBeenCalledWith('unknown');
    expect(chip.written).toEqual([]);
    // the tracker runs again
    expect(chip.resets).toBe(1);
  });
});

describe('readSettings', () => {
  it('reads the JSON of the config partition and restarts the tracker', async () => {
    chip.config = new Uint8Array([
      ...new TextEncoder().encode('{"version":1,"token":"cl_x"}'),
      0,
      0xff,
      0xff,
    ]);

    expect(
      await readSettings(port, MANIFEST, {
        onStep: () => {},
        onOutput: () => {},
      }),
    ).toEqual({ version: 1, token: 'cl_x' });
    expect(chip.resets).toBe(1);
  });
});

describe('parseSettings', () => {
  const bytes = (text: string, end = 0xff) =>
    new Uint8Array([...new TextEncoder().encode(text), end, 0xff]);

  it('reads up to the first 0x00 or 0xFF', () => {
    expect(parseSettings(bytes('{"version":1,"a":"ä"}', 0), 1)).toEqual({
      version: 1,
      a: 'ä',
    });
    expect(parseSettings(bytes('{"version":1}'), 1)).toEqual({ version: 1 });
  });

  it('takes nothing of another version, empty or broken settings', () => {
    expect(parseSettings(bytes('{"version":2}'), 1)).toBeUndefined();
    expect(parseSettings(new Uint8Array([0xff, 0xff]), 1)).toBeUndefined();
    expect(parseSettings(bytes('{"version":1'), 1)).toBeUndefined();
    expect(parseSettings(bytes('[1]'), 1)).toBeUndefined();
  });
});
