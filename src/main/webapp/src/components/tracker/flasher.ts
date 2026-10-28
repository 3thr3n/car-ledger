import { ESPLoader, Transport } from 'esptool-js';
import type { FirmwareManifest } from '@/generated';

/** a key of tracker.flash.errors */
export type FlashErrorCode =
  'connect' | 'chip' | 'download' | 'write' | 'read' | 'cancelled';

export class FlashError extends Error {
  constructor(
    readonly code: FlashErrorCode,
    readonly detail?: string,
  ) {
    super(detail ?? code);
  }
}

export type FlashStep =
  | 'connecting'
  | 'downloading'
  | 'checking'
  | 'reading'
  | 'writing'
  | 'resetting';

/**
 * install: the firmware and the settings; settings: only the settings;
 * firmware: only the firmware, the settings on the tracker stay
 */
export type FlashMode = 'install' | 'settings' | 'firmware';

/** why the partitions of the tracker may not fit the release */
export type LayoutProblem = 'different' | 'unknown';

export interface FlashOptions {
  manifest: FirmwareManifest;
  mode: FlashMode;
  /** the image of the config partition, not for the firmware only */
  config?: Uint8Array;
  /** a file of the release, e.g. firmware.bin */
  download: (path: string) => Promise<Uint8Array>;
  /**
   * The firmware only, when the partition table on the tracker isn't the one of
   * the release or can't be read: true to write anyway
   */
  confirmLayout?: (problem: LayoutProblem) => Promise<boolean>;
  onStep: (step: FlashStep) => void;
  /** 0 to 100, of all files */
  onProgress: (percent: number) => void;
  /** of esptool, then the boot output of the tracker */
  onOutput: (text: string) => void;
}

/** reads the serial output of the tracker until stopped */
export interface Monitor {
  stop: () => Promise<void>;
}

/** the tracker prints its boot output with this rate as well */
const BAUD = 115200;

/** where the ESP32 bootloader reads the partition table */
export const PARTITION_TABLE_OFFSET = 0x8000;

interface Connection {
  transport: Transport;
  loader: ESPLoader;
}

/** opens the port and starts the bootloader of the tracker */
async function connect(
  port: SerialPort,
  onOutput: (text: string) => void,
): Promise<Connection> {
  const transport = new Transport(port);
  const loader = new ESPLoader({
    transport,
    baudrate: BAUD,
    terminal: {
      clean: () => {},
      write: onOutput,
      writeLine: (line) => onOutput(line + '\n'),
    },
  });
  try {
    await loader.main();
  } catch (e) {
    await transport.disconnect().catch(() => {});
    throw new FlashError('connect', message(e));
  }
  return { transport, loader };
}

/**
 * Writes the firmware and/or the config partition, then resets the tracker.
 * Never erases the whole flash: the LittleFS partition holds the points the
 * tracker hasn't sent yet. The firmware only never writes the config partition,
 * so the settings stay.
 */
export async function flashTracker(
  port: SerialPort,
  options: FlashOptions,
): Promise<Monitor> {
  const { manifest, mode, onStep, onOutput } = options;
  onStep('connecting');
  const { transport, loader } = await connect(port, onOutput);
  try {
    const build = manifest.builds.find(
      (b) => b.chipFamily === loader.chip.CHIP_NAME,
    );
    if (!build) throw new FlashError('chip', loader.chip.CHIP_NAME);

    const files: { data: Uint8Array; address: number }[] = [];
    if (mode !== 'settings') {
      onStep('downloading');
      for (const part of build.parts) {
        try {
          files.push({
            data: await options.download(part.path),
            address: part.offset,
          });
        } catch (e) {
          throw new FlashError('download', `${part.path}: ${message(e)}`);
        }
      }
    }
    if (mode === 'firmware') {
      onStep('checking');
      const problem = await checkLayout(loader, files);
      if (problem && !(await options.confirmLayout?.(problem))) {
        throw new FlashError('cancelled');
      }
    } else {
      if (!options.config) throw new Error('No settings to write');
      files.push({ data: options.config, address: manifest.config.offset });
    }

    onStep('writing');
    const sizes = files.map((file) => file.data.length);
    const total = sizes.reduce((sum, size) => sum + size, 0);
    try {
      await loader.writeFlash({
        fileArray: files,
        flashMode: 'keep',
        flashFreq: 'keep',
        flashSize: 'keep',
        eraseAll: false,
        compress: true,
        reportProgress: (index, written, of) => {
          const before = sizes.slice(0, index).reduce((a, b) => a + b, 0);
          options.onProgress(
            Math.round(
              (100 * (before + (sizes[index] * written) / of)) / total,
            ),
          );
        },
      });
    } catch (e) {
      throw new FlashError('write', message(e));
    }
  } catch (e) {
    // a check that was cancelled leaves the tracker as it was, running again
    if (e instanceof FlashError && e.code === 'cancelled') {
      await loader.after('hard_reset').catch(() => {});
    }
    await transport.disconnect().catch(() => {});
    throw e;
  }

  onStep('resetting');
  await loader.after('hard_reset');
  return monitor(transport, onOutput);
}

/**
 * @return undefined when the partition table on the tracker is the one of the
 *   release, so its config and data partitions stay where the firmware looks
 */
async function checkLayout(
  loader: ESPLoader,
  files: { data: Uint8Array; address: number }[],
): Promise<LayoutProblem | undefined> {
  const table = files.find((file) => file.address === PARTITION_TABLE_OFFSET);
  if (!table) return 'unknown';
  try {
    const installed = await loader.readFlash(
      PARTITION_TABLE_OFFSET,
      table.data.length,
    );
    return sameBytes(installed, table.data) ? undefined : 'different';
  } catch {
    return 'unknown';
  }
}

export function sameBytes(a: Uint8Array, b: Uint8Array): boolean {
  return a.length === b.length && a.every((byte, i) => byte === b[i]);
}

/**
 * Reads the config partition of the tracker (capability read-settings) and
 * restarts it.
 *
 * @return the settings as JSON, undefined when there are none or they aren't
 *   valid JSON of the version of the release
 */
export async function readSettings(
  port: SerialPort,
  manifest: FirmwareManifest,
  options: Pick<FlashOptions, 'onStep' | 'onOutput'>,
): Promise<Record<string, unknown> | undefined> {
  options.onStep('connecting');
  const { transport, loader } = await connect(port, options.onOutput);
  try {
    options.onStep('reading');
    let data: Uint8Array;
    try {
      data = await loader.readFlash(
        manifest.config.offset,
        manifest.config.maxLength,
      );
    } catch (e) {
      throw new FlashError('read', message(e));
    }
    options.onStep('resetting');
    await loader.after('hard_reset').catch(() => {});
    return parseSettings(data, manifest.config.version);
  } finally {
    await transport.disconnect().catch(() => {});
  }
}

/**
 * The JSON of the format json-nul: up to the first 0x00 or 0xFF
 *
 * @return undefined for none, invalid JSON or another version
 */
export function parseSettings(
  data: Uint8Array,
  version: number,
): Record<string, unknown> | undefined {
  let end = data.findIndex((byte) => byte === 0x00 || byte === 0xff);
  if (end < 0) end = data.length;
  try {
    const json: unknown = JSON.parse(
      new TextDecoder('utf-8', { fatal: true }).decode(data.subarray(0, end)),
    );
    if (
      json &&
      typeof json === 'object' &&
      !Array.isArray(json) &&
      (json as { version?: unknown }).version === version
    ) {
      return json as Record<string, unknown>;
    }
  } catch {
    // empty, erased or broken: nothing to take over
  }
  return undefined;
}

/** the boot output of the tracker after the reset */
function monitor(
  transport: Transport,
  onOutput: (text: string) => void,
): Monitor {
  let stopped = false;
  const decoder = new TextDecoder();
  const reading = transport
    .rawRead(
      (data) => onOutput(decoder.decode(data, { stream: true })),
      () => stopped,
    )
    .catch(() => {});
  return {
    stop: async () => {
      stopped = true;
      await transport.disconnect().catch(() => {});
      await reading;
    },
  };
}

const message = (e: unknown) => (e instanceof Error ? e.message : String(e));
