import type { ConfigPartition } from '@/generated';

/** title and description of a field, per language, e.g. de */
export type Translations = Record<
  string,
  { title?: string; description?: string } | undefined
>;

interface Texts {
  title?: string;
  description?: string;
  translations?: Translations;
}

/**
 * A setting of the firmware, the subset of JSON Schema of the spec
 * (docs/TRACKER_FIRMWARE.md)
 */
export interface ScalarField extends Texts {
  type: 'string' | 'integer' | 'number' | 'boolean';
  default?: string | number | boolean;
  minLength?: number;
  maxLength?: number;
  /** UTF-8 bytes */
  minBytes?: number;
  maxBytes?: number;
  pattern?: string;
  /** password, pem, uri; others are plain text */
  format?: string;
  minimum?: number;
  maximum?: number;
  /** shown folded away */
  advanced?: boolean;
}

export interface ItemSchema extends Texts {
  type: 'object';
  properties: Record<string, ScalarField>;
  required?: string[];
}

export interface ListField extends Texts {
  type: 'array';
  items: ItemSchema;
  minItems?: number;
  maxItems?: number;
  advanced?: boolean;
}

export type Field = ScalarField | ListField;

export interface SettingsSchema {
  type: 'object';
  properties: Record<string, Field>;
  required?: string[];
}

/** filled by CarLedger, not rendered from the schema */
export const RESERVED = ['url', 'token'];

export type Config = Pick<ConfigPartition, 'maxLength' | 'version' | 'schema'>;

/** the manifest is checked against the schema of the spec on the server */
export const settingsSchema = (config: Pick<ConfigPartition, 'schema'>) =>
  config.schema as unknown as SettingsSchema;

/** as typed: text for strings and numbers */
export type Value = string | boolean;
export type Item = Record<string, Value>;

/**
 * The settings of a tracker, written as JSON into its config partition, so the
 * same firmware fits every tracker
 */
export interface TrackerSettings {
  url: string;
  token: string;
  /** the fields of the schema, without the reserved ones */
  values: Record<string, Value | Item[]>;
}

/** a key of tracker.flash.errors and its values */
export interface SettingsError {
  key: string;
  values?: Record<string, unknown>;
}

/**
 * by path, e.g. url, wifi, wifi.0.ssid; size for settings that don't fit into
 * the partition
 */
export type SettingsErrors = Record<string, SettingsError>;

const TOKEN = /^cl_[\w-]{43}$/;
const PEM =
  /^-----BEGIN CERTIFICATE-----\n[A-Za-z0-9+/=\n]+\n-----END CERTIFICATE-----\n$/;

const bytes = (text: string) => new TextEncoder().encode(text).length;

export const isList = (field: Field): field is ListField =>
  field.type === 'array';

const fields = (schema: SettingsSchema) =>
  Object.entries(schema.properties).filter(
    ([name]) => !RESERVED.includes(name),
  );

function initialValue(field: ScalarField): Value {
  if (field.type === 'boolean') return field.default === true;
  return field.default === undefined ? '' : String(field.default);
}

export function emptyItem(items: ItemSchema): Item {
  return Object.fromEntries(
    Object.entries(items.properties).map(([name, field]) => [
      name,
      initialValue(field),
    ]),
  );
}

/** the defaults of the schema, a list with its least number of items */
export function initialSettings(
  schema: SettingsSchema,
  url: string,
  token: string,
): TrackerSettings {
  return {
    url,
    token,
    values: Object.fromEntries(
      fields(schema).map(([name, field]) => [
        name,
        isList(field)
          ? Array.from({ length: Math.max(field.minItems ?? 0, 1) }, () =>
              emptyItem(field.items),
            )
          : initialValue(field),
      ]),
    ),
  };
}

/** the value of the form for what the tracker stored, else the default */
function storedValue(field: ScalarField, stored: unknown): Value {
  if (field.type === 'boolean') {
    return typeof stored === 'boolean' ? stored : initialValue(field);
  }
  if (typeof stored === 'string' || typeof stored === 'number') {
    return String(stored);
  }
  // left out: empty, e.g. the password of an open network
  return '';
}

/**
 * The settings read back from the tracker (capability read-settings) as the
 * form: the fields of the schema, anything else is left out
 *
 * @param token the token of a tracker just created wins over the stored one
 */
export function storedSettings(
  schema: SettingsSchema,
  stored: Record<string, unknown>,
  current: TrackerSettings,
  token?: string,
): TrackerSettings {
  const values = Object.fromEntries(
    fields(schema).map(([name, field]) => {
      const value = stored[name];
      if (!isList(field)) return [name, storedValue(field, value)];
      const items = Array.isArray(value)
        ? value
            .filter((item) => item && typeof item === 'object')
            .map((item: Record<string, unknown>) =>
              Object.fromEntries(
                Object.entries(field.items.properties).map(
                  ([itemName, itemField]) => [
                    itemName,
                    storedValue(itemField, item[itemName]),
                  ],
                ),
              ),
            )
        : [];
      return [name, items.length ? items : current.values[name]];
    }),
  );
  return {
    url: typeof stored.url === 'string' ? stored.url : current.url,
    token:
      token ??
      (typeof stored.token === 'string' ? stored.token : current.token),
    values,
  };
}

/** one certificate with \n line ends and a final \n, empty for none */
function normalizePem(ca: string): string {
  const trimmed = ca.replace(/\r\n?/g, '\n').trim();
  return trimmed ? trimmed + '\n' : '';
}

/** what the firmware stores, undefined for an empty field */
function encode(field: ScalarField, value: Value | undefined): unknown {
  if (field.type === 'boolean') return value === true;
  const text = typeof value === 'string' ? value : '';
  if (field.type === 'integer' || field.type === 'number') {
    return text.trim() === '' ? undefined : Number(text);
  }
  const encoded = field.format === 'pem' ? normalizePem(text) : text;
  return encoded === '' ? undefined : encoded;
}

function encodeItem(items: ItemSchema, item: Item) {
  return Object.fromEntries(
    Object.entries(items.properties)
      .map(([name, field]) => [name, encode(field, item[name])])
      .filter(([, value]) => value !== undefined),
  );
}

/** the version first, then the fields in the order of the schema */
function json(settings: TrackerSettings, config: Config): string {
  const schema = settingsSchema(config);
  const result: Record<string, unknown> = { version: config.version };
  for (const [name, field] of Object.entries(schema.properties)) {
    const value =
      name === 'url'
        ? settings.url.trim()
        : name === 'token'
          ? settings.token.trim()
          : isList(field)
            ? (settings.values[name] as Item[]).map((item) =>
                encodeItem(field.items, item),
              )
            : encode(field, settings.values[name] as Value);
    if (value !== undefined) result[name] = value;
  }
  return JSON.stringify(result);
}

/** {prefix}Between, {prefix}Min or {prefix}Max, undefined within the range */
function range(
  prefix: string,
  value: number,
  min: number | undefined,
  max: number | undefined,
): SettingsError | undefined {
  if (
    (min === undefined || value >= min) &&
    (max === undefined || value <= max)
  )
    return undefined;
  const key =
    min !== undefined && max !== undefined
      ? 'Between'
      : min !== undefined
        ? 'Min'
        : 'Max';
  return { key: prefix + key, values: { min, max } };
}

function matches(pattern: string, text: string): boolean {
  try {
    return new RegExp(pattern, 'u').test(text);
  } catch {
    // a pattern this browser can't read isn't checked
    return true;
  }
}

function validateValue(
  field: ScalarField,
  value: Value | undefined,
  required: boolean,
): SettingsError | undefined {
  if (field.type === 'boolean') return undefined;
  const text = typeof value === 'string' ? value : '';
  if (text.trim() === '') return required ? { key: 'required' } : undefined;

  if (field.type === 'integer' || field.type === 'number') {
    const number = Number(text);
    if (
      field.type === 'integer' ? !/^\s*-?\d+\s*$/.test(text) : !isFinite(number)
    )
      return { key: field.type };
    return range('range', number, field.minimum, field.maximum);
  }

  if (field.format === 'pem' && !PEM.test(normalizePem(text)))
    return { key: 'pem' };
  if (field.format === 'uri' && !isUrl(text.trim())) return { key: 'uri' };
  return (
    range('bytes', bytes(text), field.minBytes, field.maxBytes) ??
    range('length', [...text].length, field.minLength, field.maxLength) ??
    (field.pattern && !matches(field.pattern, text)
      ? { key: 'pattern' }
      : undefined)
  );
}

/**
 * Checks the settings against the schema of the firmware, and the reserved ones
 * like CarLedger expects them
 */
export function validateSettings(
  settings: TrackerSettings,
  config: Config,
): SettingsErrors {
  const schema = settingsSchema(config);
  const errors: SettingsErrors = {};
  const required = new Set(schema.required ?? []);

  if (!isUrl(settings.url.trim())) errors.url = { key: 'url' };
  if (!TOKEN.test(settings.token.trim())) errors.token = { key: 'token' };

  for (const [name, field] of fields(schema)) {
    if (isList(field)) {
      const items = settings.values[name] as Item[];
      const count = range(
        'items',
        items.length,
        field.minItems,
        field.maxItems,
      );
      if (count) errors[name] = count;
      const requiredInItem = new Set(field.items.required ?? []);
      items.forEach((item, index) => {
        for (const [itemName, itemField] of Object.entries(
          field.items.properties,
        )) {
          const error = validateValue(
            itemField,
            item[itemName],
            requiredInItem.has(itemName),
          );
          if (error) errors[`${name}.${index}.${itemName}`] = error;
        }
      });
    } else {
      const error = validateValue(
        field,
        settings.values[name] as Value,
        required.has(name),
      );
      if (error) errors[name] = error;
    }
  }

  // the JSON and its NUL
  if (bytes(json(settings, config)) + 1 > config.maxLength) {
    errors.size = { key: 'tooLarge', values: { max: config.maxLength } };
  }
  return errors;
}

export function hasErrors(errors: SettingsErrors): boolean {
  return Object.keys(errors).length > 0;
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
 * The image of the config partition, format json-nul: the JSON, one NUL, 0xFF
 * up to a multiple of 4 bytes. The firmware reads up to the first 0x00 or 0xFF.
 */
export function configImage(
  settings: TrackerSettings,
  config: Config,
): Uint8Array {
  const data = new TextEncoder().encode(json(settings, config));
  const image = new Uint8Array(Math.ceil((data.length + 1) / 4) * 4).fill(0xff);
  image.set(data);
  image[data.length] = 0x00;
  return image;
}
