import { describe, expect, it } from 'vitest';
import {
  frequencyColor,
  frequencyLines,
  frequencyWidth,
} from './frequencyScale';

describe('frequencyLines', () => {
  it('keeps the passes and the order of the lines', () => {
    const lines = frequencyLines({
      maxPasses: 3,
      lines: [
        {
          passes: 1,
          last: '2026-10-20T08:00:00Z',
          coordinates: [
            [10, 50],
            [10, 50.01],
          ],
        },
        {
          passes: 3,
          last: '2026-10-21T08:00:00Z',
          coordinates: [
            [10, 50.01],
            [10, 50.02],
          ],
        },
      ],
    });

    expect(lines.features.map((line) => line.properties?.passes)).toEqual([
      1, 3,
    ]);
    expect(lines.features[1].geometry.coordinates).toEqual([
      [10, 50.01],
      [10, 50.02],
    ]);
  });
});

describe('frequencyColor', () => {
  it('spreads the colors from once to the most passes', () => {
    expect(frequencyColor(7).slice(3)).toEqual([
      1,
      '#feb24c',
      3,
      '#fc4e2a',
      5,
      '#bd0026',
      7,
      '#800026',
    ]);
  });

  it('has a range when every road was passed once', () => {
    expect(frequencyColor(1).slice(-2)).toEqual([2, '#800026']);
    expect(frequencyWidth(0).slice(-2)).toEqual([2, 9]);
  });
});
