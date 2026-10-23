import { ESPLoader, Transport } from 'esptool-js';
import type { FirmwareManifest } from '@/generated';

/** a key of tracker.flash.errors */
export type FlashErrorCode = 'connect' | 'chip' | 'download' | 'write';

export class FlashError extends Error {
  constructor(
    readonly code: FlashErrorCode,
    readonly detail?: string,
  ) {
    super(detail ?? code);
  }
}

export type FlashStep = 'connecting' | 'downloading' | 'writing' | 'resetting';

export interface FlashOptions {
  manifest: FirmwareManifest;
  /** the firmware as well, or only the settings */
  install: boolean;
  config: Uint8Array;
  /** a file of the release, e.g. firmware.bin */
  download: (path: string) => Promise<Uint8Array>;
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

/**
 * Writes the firmware and/or the config partition, then resets the tracker.
 * Never erases the whole flash: the LittleFS partition holds the points the
 * tracker hasn't sent yet.
 */
export async function flashTracker(
  port: SerialPort,
  options: FlashOptions,
): Promise<Monitor> {
  const { manifest, onStep, onOutput } = options;
  const transport = new Transport(port);
  try {
    onStep('connecting');
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
      throw new FlashError('connect', message(e));
    }

    const build = manifest.builds.find(
      (b) => b.chipFamily === loader.chip.CHIP_NAME,
    );
    if (!build) throw new FlashError('chip', loader.chip.CHIP_NAME);

    const files = [];
    if (options.install) {
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
    files.push({ data: options.config, address: manifest.config.offset });

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

    onStep('resetting');
    await loader.after('hard_reset');
  } catch (e) {
    await transport.disconnect().catch(() => {});
    throw e;
  }

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
