import { describe, expect, it } from 'vitest';
import { scaleOf } from './gauge';

describe('scaleOf', () => {
  it('rounds to the step with room at both ends', () => {
    expect(scaleOf([4.97, 6.01, 7.35], 1)).toEqual({ min: 3, max: 9 });
  });

  it('keeps at least one step of room for a single value', () => {
    expect(scaleOf([1549], 500)).toEqual({ min: 1000, max: 2500 });
  });

  it('does not go below zero', () => {
    expect(scaleOf([200, 400], 500)).toEqual({ min: 0, max: 1000 });
  });
});
