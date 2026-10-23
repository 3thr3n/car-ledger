import type { ConfigPartition } from '@/generated';

/**
 * The settings of a tracker, written as JSON into its config partition, so the
 * same firmware fits every tracker
 */
export interface TrackerSettings {
  /** 2.4 GHz only, the tracker tries them in this order */
  wifi: { ssid: string; pass: string }[];
  url: string;
  token: string;
  /** PEM of a private or self-signed CA, else the built-in CA bundle */
  ca?: string;
}

/** a key of tracker.flash.errors */
export type SettingsError =
  'wifi' | 'ssid' | 'pass' | 'url' | 'token' | 'ca' | 'tooLarge';

export interface SettingsErrors {
  wifi?: SettingsError;
  /** per network, same order as the networks */
  networks: { ssid?: SettingsError; pass?: SettingsError }[];
  url?: SettingsError;
  token?: SettingsError;
  ca?: SettingsError;
  size?: SettingsError;
}

const TOKEN = /^cl_[\w-]{43}$/;
const PEM =
  /^-----BEGIN CERTIFICATE-----\n[A-Za-z0-9+/=\n]+\n-----END CERTIFICATE-----\n$/;

const bytes = (text: string) => new TextEncoder().encode(text).length;

/** what the firmware stores: trimmed, without empty optional fields */
function json(settings: TrackerSettings, version: number): string {
  const ca = normalizePem(settings.ca);
  return JSON.stringify({
    version,
    wifi: settings.wifi.map(({ ssid, pass }) => ({ ssid, pass })),
    url: settings.url.trim(),
    token: settings.token.trim(),
    ...(ca && { ca }),
  });
}

/** one certificate with \n line ends and a final \n, empty for none */
function normalizePem(ca: string | undefined): string | undefined {
  const trimmed = ca?.replace(/\r\n?/g, '\n').trim();
  return trimmed ? trimmed + '\n' : undefined;
}

/**
 * Checks the settings like the firmware does, which rejects an invalid config
 * and doesn't upload then
 */
export function validateSettings(
  settings: TrackerSettings,
  config: Pick<ConfigPartition, 'maxLength' | 'version'>,
): SettingsErrors {
  const errors: SettingsErrors = {
    networks: settings.wifi.map(({ ssid, pass }) => ({
      ssid: bytes(ssid) < 1 || bytes(ssid) > 32 ? 'ssid' : undefined,
      pass:
        pass !== '' && (bytes(pass) < 8 || bytes(pass) > 63)
          ? 'pass'
          : undefined,
    })),
  };
  if (settings.wifi.length === 0) errors.wifi = 'wifi';
  if (!isUrl(settings.url.trim())) errors.url = 'url';
  if (!TOKEN.test(settings.token.trim())) errors.token = 'token';
  const ca = normalizePem(settings.ca);
  if (ca && !PEM.test(ca)) errors.ca = 'ca';
  // the JSON and its NUL
  if (bytes(json(settings, config.version)) + 1 > config.maxLength) {
    errors.size = 'tooLarge';
  }
  return errors;
}

export function hasErrors(errors: SettingsErrors): boolean {
  return (
    Object.entries(errors).some(
      ([key, value]) => key !== 'networks' && value,
    ) || errors.networks.some((network) => network.ssid || network.pass)
  );
}

/** http:// only for local tests, the tracker sends its token with it */
function isUrl(url: string): boolean {
  if (!/^https?:\/\//.test(url)) return false;
  try {
    return !!new URL(url).host;
  } catch {
    return false;
  }
}

/**
 * The image of the config partition: the JSON, one NUL, 0xFF up to a multiple
 * of 4 bytes. The firmware reads up to the first 0x00 or 0xFF.
 */
export function configImage(
  settings: TrackerSettings,
  config: Pick<ConfigPartition, 'version'>,
): Uint8Array {
  const data = new TextEncoder().encode(json(settings, config.version));
  const image = new Uint8Array(Math.ceil((data.length + 1) / 4) * 4).fill(0xff);
  image.set(data);
  image[data.length] = 0x00;
  return image;
}
