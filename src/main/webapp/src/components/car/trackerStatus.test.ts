import { describe, expect, it } from 'vitest';
import { storagePercent, trackerWarnings } from './trackerStatus';

describe('trackerWarnings', () => {
  it('warns about the storage, the settings and another firmware', () => {
    expect(
      trackerWarnings(
        { storageError: true, configOk: false, firmware: 'v1.0.0' },
        'v1.1.0',
      ),
    ).toEqual(['storageError', 'config', 'firmware']);
  });

  it('is quiet when all is well or unknown', () => {
    expect(
      trackerWarnings(
        { storageError: false, configOk: true, firmware: 'v1.1.0' },
        'v1.1.0',
      ),
    ).toEqual([]);
    // no firmware on the server, or none reported
    expect(trackerWarnings({ firmware: 'v1.0.0' })).toEqual([]);
    expect(trackerWarnings({}, 'v1.1.0')).toEqual([]);
  });
});

describe('storagePercent', () => {
  it('needs both values', () => {
    expect(storagePercent({ storageUsed: 512, storageTotal: 2048 })).toBe(25);
    expect(storagePercent({ storageUsed: 0, storageTotal: 2048 })).toBe(0);
    expect(storagePercent({ storageUsed: 512 })).toBeUndefined();
    expect(storagePercent({ storageTotal: 2048 })).toBeUndefined();
  });
});
