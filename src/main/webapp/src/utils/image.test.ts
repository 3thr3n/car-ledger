import { describe, expect, it } from 'vitest';
import { asJpegName, fitInto } from './image';

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
});
