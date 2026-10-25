import { describe, expect, it } from 'vitest';
import { restoredParts } from './restoredParts';

describe('the message after a restore', () => {
  it('names what was restored, in its order, without zeros', () => {
    expect(
      restoredParts({
        files: 4,
        cars: 1,
        trips: 0,
        modifications: 2,
        bills: 12,
        technicalInspections: 1,
        trackPoints: 3000,
      }),
    ).toEqual([
      { part: 'cars', count: 1 },
      { part: 'bills', count: 12 },
      { part: 'modifications', count: 2 },
      { part: 'technicalInspections', count: 1 },
      { part: 'files', count: 4 },
    ]);
  });

  it('names nothing of an empty backup', () => {
    expect(restoredParts({ cars: 0 })).toEqual([]);
  });
});
