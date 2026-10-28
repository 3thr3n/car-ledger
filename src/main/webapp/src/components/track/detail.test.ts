import { describe, expect, it } from 'vitest';
import { detailQuery, metersPerPixel, needsDetail } from './detail';

const CITY = { zoom: 15, west: 9.99, south: 49.99, east: 10.01, north: 50.01 };

describe('detail', () => {
  it('measures a pixel like the server', () => {
    expect(metersPerPixel(CITY)).toBeCloseTo(1.534, 3);
    expect(metersPerPixel({ ...CITY, zoom: 16 })).toBeCloseTo(0.767, 3);
  });

  it('needs the detail when the lines are more than 2 pixels off', () => {
    // 3 days: 15 m
    expect(needsDetail(15, CITY)).toBe(true);
    // about 6 m per pixel
    expect(needsDetail(15, { ...CITY, zoom: 13 })).toBe(true);
    // about 12 m per pixel
    expect(needsDetail(15, { ...CITY, zoom: 12 })).toBe(false);
    expect(needsDetail(undefined, CITY)).toBe(false);
    expect(needsDetail(15, undefined)).toBe(false);
  });

  it('asks for the view with a margin, rounded', () => {
    expect(detailQuery({ ...CITY, zoom: 15.234 })).toEqual({
      bbox: '9.98,49.98,10.02,50.02',
      zoom: 15.2,
    });
    // not beyond the earth
    expect(
      detailQuery({ zoom: 2, west: -170, south: -80, east: 170, north: 80 })
        .bbox,
    ).toBe('-180,-90,180,90');
  });
});
