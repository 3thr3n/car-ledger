import { describe, expect, it } from 'vitest';
import {
  Config,
  configImage,
  hasErrors,
  initialSettings,
  storedSettings,
  SettingsSchema,
  TrackerSettings,
  validateSettings,
} from './configImage';

const TOKEN = 'cl_' + 'a'.repeat(43);
const SERVER = 'https://carledger.example.com/api/device/points';
const PEM =
  '-----BEGIN CERTIFICATE-----\nMIIBszCCAVmgAwIBAgIU\nAbc+/==\n-----END CERTIFICATE-----\n';

/** as in src/test/resources/firmware/manifest.json */
const SCHEMA: SettingsSchema = {
  type: 'object',
  required: ['wifi', 'url', 'token'],
  properties: {
    wifi: {
      type: 'array',
      title: 'WLAN networks',
      minItems: 1,
      maxItems: 8,
      items: {
        type: 'object',
        title: 'Network',
        required: ['ssid'],
        properties: {
          ssid: { type: 'string', minBytes: 1, maxBytes: 32 },
          pass: {
            type: 'string',
            format: 'password',
            minBytes: 8,
            maxBytes: 63,
          },
        },
      },
    },
    url: { type: 'string' },
    token: { type: 'string' },
    ca: { type: 'string', format: 'pem', advanced: true },
  },
};

const config = (
  properties: SettingsSchema['properties'] = {},
  required: string[] = [],
): Config => ({
  maxLength: 8192,
  version: 1,
  schema: {
    ...SCHEMA,
    properties: { ...SCHEMA.properties, ...properties },
    required: [...SCHEMA.required!, ...required],
  } as unknown as Config['schema'],
});

const CONFIG = config();

const settings = (
  values: TrackerSettings['values'] = {},
  overrides: Partial<TrackerSettings> = {},
): TrackerSettings => ({
  url: SERVER,
  token: TOKEN,
  values: {
    wifi: [{ ssid: 'Home', pass: 'secret-password' }],
    ca: '',
    ...values,
  },
  ...overrides,
});

const text = (image: Uint8Array) =>
  new TextDecoder().decode(image.subarray(0, image.indexOf(0)));

describe('initialSettings', () => {
  it('starts with the defaults and one item of a list', () => {
    const initial = initialSettings(
      {
        type: 'object',
        properties: {
          ...SCHEMA.properties,
          interval: { type: 'integer', default: 10 },
          led: { type: 'boolean', default: true },
          debug: { type: 'boolean' },
        },
      },
      SERVER,
      TOKEN,
    );

    expect(initial).toEqual({
      url: SERVER,
      token: TOKEN,
      values: {
        wifi: [{ ssid: '', pass: '' }],
        ca: '',
        interval: '10',
        led: true,
        debug: false,
      },
    });
  });
});

describe('configImage', () => {
  it('writes the JSON, a NUL and pads with 0xFF to 4 bytes', () => {
    const image = configImage(settings(), CONFIG);
    const end = image.indexOf(0);

    expect(text(image)).toBe(
      JSON.stringify({
        version: 1,
        wifi: [{ ssid: 'Home', pass: 'secret-password' }],
        url: SERVER,
        token: TOKEN,
      }),
    );
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

  it('encodes UTF-8 and leaves out empty fields', () => {
    const image = configImage(
      settings({ wifi: [{ ssid: 'Café ☕', pass: '' }], ca: '  ' }),
      CONFIG,
    );
    const json = JSON.parse(text(image));

    // an open network
    expect(json.wifi).toEqual([{ ssid: 'Café ☕' }]);
    expect(json).not.toHaveProperty('ca');
  });

  it('writes the certificate with \\n line ends', () => {
    const image = configImage(
      settings({ ca: PEM.replace(/\n/g, '\r\n').trim() }),
      CONFIG,
    );

    expect(JSON.parse(text(image)).ca).toBe(PEM);
  });

  it('writes numbers and booleans of the schema', () => {
    const image = configImage(
      settings({ interval: ' 15 ', ratio: '0.5', led: false, limit: '' }),
      config({
        interval: { type: 'integer' },
        ratio: { type: 'number' },
        led: { type: 'boolean' },
        limit: { type: 'integer' },
      }),
    );

    expect(JSON.parse(text(image))).toMatchObject({
      interval: 15,
      ratio: 0.5,
      led: false,
    });
    expect(JSON.parse(text(image))).not.toHaveProperty('limit');
  });
});

describe('validateSettings', () => {
  it('accepts valid settings, an open network and http for local tests', () => {
    const errors = validateSettings(
      settings(
        {
          wifi: [
            { ssid: 'Home', pass: 'secret-password' },
            { ssid: 'Open', pass: '' },
          ],
          ca: PEM,
        },
        { url: 'http://192.168.1.10:8080/api/device/points' },
      ),
      CONFIG,
    );

    expect(hasErrors(errors)).toBe(false);
  });

  it('checks the number of items of a list', () => {
    expect(validateSettings(settings({ wifi: [] }), CONFIG).wifi).toEqual({
      key: 'itemsBetween',
      values: { min: 1, max: 8 },
    });
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
    const bytes = { key: 'bytesBetween', values: { min: 8, max: 63 } };

    expect(errors).toEqual({
      'wifi.0.ssid': { key: 'required' },
      'wifi.0.pass': bytes,
      'wifi.1.ssid': { key: 'bytesBetween', values: { min: 1, max: 32 } },
      'wifi.1.pass': bytes,
    });
  });

  it('checks SERVER, token and certificate', () => {
    const errors = validateSettings(
      settings(
        { ca: 'not a certificate' },
        { url: 'ftp://example.com', token: 'cl_short' },
      ),
      CONFIG,
    );

    expect(errors).toEqual({
      url: { key: 'url' },
      token: { key: 'token' },
      ca: { key: 'pem' },
    });
    expect(
      validateSettings(settings({}, { url: 'https://' }), CONFIG).url,
    ).toEqual({ key: 'url' });
  });

  it('checks the rules of the schema', () => {
    const errors = validateSettings(
      settings({
        interval: '1.5',
        ratio: 'abc',
        level: '11',
        name: 'ab',
        code: 'abc',
        server: 'nope',
        host: '',
      }),
      config(
        {
          interval: { type: 'integer' },
          ratio: { type: 'number' },
          level: { type: 'integer', minimum: 0, maximum: 10 },
          name: { type: 'string', minLength: 3 },
          code: { type: 'string', pattern: '^[0-9]+$' },
          server: { type: 'string', format: 'uri' },
          host: { type: 'string' },
        },
        ['host'],
      ),
    );

    expect(errors).toEqual({
      interval: { key: 'integer' },
      ratio: { key: 'number' },
      level: { key: 'rangeBetween', values: { min: 0, max: 10 } },
      name: { key: 'lengthMin', values: { min: 3, max: undefined } },
      code: { key: 'pattern' },
      server: { key: 'uri' },
      host: { key: 'required' },
    });
  });

  it('fits the JSON and its NUL into the partition', () => {
    const fitting = configImage(settings(), CONFIG).indexOf(0) + 1;

    expect(
      validateSettings(settings(), { ...CONFIG, maxLength: fitting }).size,
    ).toBeUndefined();
    expect(
      validateSettings(settings(), { ...CONFIG, maxLength: fitting - 1 }).size,
    ).toEqual({ key: 'tooLarge', values: { max: fitting - 1 } });
  });
});

describe('storedSettings', () => {
  const current = initialSettings(SCHEMA, SERVER, '');

  it('takes over the settings of the tracker', () => {
    const stored = storedSettings(
      SCHEMA,
      {
        version: 1,
        wifi: [{ ssid: 'Home', pass: 'secret-password' }, { ssid: 'Open' }],
        url: 'https://other.example.com/api/device/points',
        token: TOKEN,
        ca: PEM,
        unknown: 'left out',
      },
      current,
    );

    expect(stored).toEqual({
      url: 'https://other.example.com/api/device/points',
      token: TOKEN,
      values: {
        wifi: [
          { ssid: 'Home', pass: 'secret-password' },
          { ssid: 'Open', pass: '' },
        ],
        ca: PEM,
      },
    });
  });

  it('keeps the token of a new tracker and the form without values', () => {
    const fresh = 'cl_' + 'b'.repeat(43);
    const stored = storedSettings(
      SCHEMA,
      { version: 1, token: TOKEN, wifi: 'not a list' },
      current,
      fresh,
    );

    expect(stored.token).toBe(fresh);
    expect(stored.url).toBe(SERVER);
    expect(stored.values.wifi).toEqual(current.values.wifi);
    expect(stored.values.ca).toBe('');
  });
});
