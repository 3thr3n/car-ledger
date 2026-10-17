import { describe, expect, it } from 'vitest';
import { keyBytes, keyMatches } from '@/utils/push';

const SERVER_KEY =
  'BMr4sovX-Knztd4LMqFOnJIjz9FXSPvXIdeC0nVWWopB_TlR-t6vigAmveTcfMdwsTCDANL9YCs8XPml0L9Sq_8';
const OTHER_KEY =
  'BCJSDWZK6-blF3MTma2C9yIrd_YIX5wBSG-zBYbKaRlW5djEZ7bz4zWow9OZ3HA8cKRsC37RycXhLcjGHK71WRo';

describe('key of the push subscription', () => {
  it('decodes the key of the server', () => {
    const bytes = keyBytes(SERVER_KEY);
    expect(bytes).toHaveLength(65);
    expect(bytes[0]).toBe(4);
  });

  it('keeps a subscription with the key of the server', () => {
    expect(keyMatches(keyBytes(SERVER_KEY).buffer, SERVER_KEY)).toBe(true);
  });

  it('renews a subscription with an older key', () => {
    expect(keyMatches(keyBytes(OTHER_KEY).buffer, SERVER_KEY)).toBe(false);
  });

  it('keeps a subscription when the browser does not tell its key', () => {
    expect(keyMatches(null, SERVER_KEY)).toBeUndefined();
    expect(keyMatches(undefined, SERVER_KEY)).toBeUndefined();
    expect(keyMatches(new ArrayBuffer(0), SERVER_KEY)).toBeUndefined();
  });
});
