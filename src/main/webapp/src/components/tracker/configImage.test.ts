import { describe, expect, it } from 'vitest';
import {
  configImage,
  hasErrors,
  TrackerSettings,
  validateSettings,
} from './configImage';

const TOKEN = 'cl_' + 'a'.repeat(43);
const CONFIG = { maxLength: 8192, version: 1 };
const PEM =
  '-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIU\nAbc+/==\n-----END CERTIFICATE-----\n';

const settings = (
  overrides: Partial<TrackerSettings> = {},
): TrackerSettings => ({
  wifi: [{ ssid: 'Home', pass: 'secret-password' }],
  url: 'https://carledger.example.com/api/device/points',
  token: TOKEN,
  ...overrides,
});

const text = (image: Uint8Array) =>
  new TextDecoder().decode(image.subarray(0, image.indexOf(0)));

describe('configImage', () => {
  it('writes the JSON, a NUL and pads with 0xFF to 4 bytes', () => {
    const image = configImage(settings(), CONFIG);
    const end = image.indexOf(0);

    expect(JSON.parse(text(image))).toEqual({
      version: 1,
      wifi: [{ ssid: 'Home', pass: 'secret-password' }],
      url: 'https://carledger.example.com/api/device/points',
      token: TOKEN,
    });
    expect(image.length % 4).toBe(0);
    expect(image.length - end).toBeLessThanOrEqual(4);
    expect([...image.subarray(end + 1)].every((b) => b === 0xff)).toBe(true);
  });

  it('ends with the NUL when the JSON fills the 4 bytes', () => {
    for (let i = 0; i < 4; i++) {
      const image = configImage(
        settings({ wifi: [{ ssid: 'H'.repeat(i + 1), pass: '' }] }),
        CONFIG,
      );
      expect(image.length % 4).toBe(0);
      expect(image.indexOf(0)).toBeGreaterThanOrEqual(image.length - 4);
    }
  });

  it('encodes UTF-8 and leaves out an empty certificate', () => {
    const image = configImage(
      settings({ wifi: [{ ssid: 'Café ☕', pass: '' }], ca: '  ' }),
      CONFIG,
    );
    const json = JSON.parse(text(image));

    expect(json.wifi[0].ssid).toBe('Café ☕');
    expect(json).not.toHaveProperty('ca');
  });

  it('writes the certificate with \\n line ends', () => {
    const image = configImage(
      settings({ ca: PEM.replace(/\n/g, '\r\n').trim() }),
      CONFIG,
    );

    expect(JSON.parse(text(image)).ca).toBe(PEM);
  });
});

describe('validateSettings', () => {
  it('accepts valid settings, an open network and http for local tests', () => {
    const errors = validateSettings(
      settings({
        wifi: [
          { ssid: 'Home', pass: 'secret-password' },
          { ssid: 'Open', pass: '' },
        ],
        url: 'http://192.168.1.10:8080/api/device/points',
        ca: PEM,
      }),
      CONFIG,
    );

    expect(hasErrors(errors)).toBe(false);
  });

  it('needs a network', () => {
    const errors = validateSettings(settings({ wifi: [] }), CONFIG);

    expect(errors.wifi).toBe('wifi');
    expect(hasErrors(errors)).toBe(true);
  });

  it('counts the bytes of SSID and password', () => {
    const errors = validateSettings(
      settings({
        wifi: [
          { ssid: '', pass: 'short' },
          // 11 × 3 bytes
          { ssid: '☕'.repeat(11), pass: 'x'.repeat(64) },
          { ssid: '☕'.repeat(10), pass: 'x'.repeat(63) },
        ],
      }),
      CONFIG,
    );

    expect(errors.networks).toEqual([
      { ssid: 'ssid', pass: 'pass' },
      { ssid: 'ssid', pass: 'pass' },
      { ssid: undefined, pass: undefined },
    ]);
  });

  it('checks URL, token and certificate', () => {
    const errors = validateSettings(
      settings({
        url: 'ftp://example.com',
        token: 'cl_short',
        ca: 'not a certificate',
      }),
      CONFIG,
    );

    expect(errors).toMatchObject({ url: 'url', token: 'token', ca: 'ca' });
    expect(validateSettings(settings({ url: 'https://' }), CONFIG).url).toBe(
      'url',
    );
  });

  it('fits the JSON and its NUL into the partition', () => {
    const fitting = configImage(settings(), CONFIG).indexOf(0) + 1;

    expect(
      validateSettings(settings(), { ...CONFIG, maxLength: fitting }).size,
    ).toBeUndefined();
    expect(
      validateSettings(settings(), { ...CONFIG, maxLength: fitting - 1 }).size,
    ).toBe('tooLarge');
  });
});
