import { describe, expect, it } from 'vitest';
import { asJpegName, centerSquare, fitInto } from './image';

describe('image', () => {
  it('scales the longest side down to the maximum', () => {
    expect(fitInto(4032, 3024)).toEqual({ width: 2000, height: 1500 });
    expect(fitInto(3024, 4032)).toEqual({ width: 1500, height: 2000 });
  });

  it('does not scale small images up', () => {
    expect(fitInto(800, 600)).toEqual({ width: 800, height: 600 });
  });

  it('names the converted photo as JPEG', () => {
    expect(asJpegName('IMG_1234.HEIC')).toBe('IMG_1234.jpg');
    expect(asJpegName('receipt.from.shop.png')).toBe('receipt.from.shop.jpg');
    expect(asJpegName('photo')).toBe('photo.jpg');
    expect(asJpegName('.jpeg')).toBe('photo.jpg');
  });

  it('crops the centered square', () => {
    expect(centerSquare(4000, 3000)).toEqual({
      x: 500,
      y: 0,
      width: 3000,
      height: 3000,
    });
    expect(centerSquare(1000, 1501)).toEqual({
      x: 0,
      y: 250,
      width: 1000,
      height: 1000,
    });
  });
});
