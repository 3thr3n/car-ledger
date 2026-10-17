/** longest side of uploaded photos, enough to read a receipt */
export const MAX_SIDE = 2000;
/** smaller PNG and WebP files are kept, e.g. screenshots of a receipt */
const KEEP_BELOW = 1.5 * 1024 * 1024;
const JPEG_QUALITY = 0.85;

/**
 * @return the size scaled down to fit into a square of `max`, never scaled up
 */
export function fitInto(width: number, height: number, max = MAX_SIDE) {
  const scale = Math.min(1, max / Math.max(width, height));
  return {
    width: Math.round(width * scale),
    height: Math.round(height * scale),
  };
}

/** `IMG_1234.HEIC` → `IMG_1234.jpg` */
export function asJpegName(name: string) {
  return `${name.replace(/\.[^./\\]*$/, '') || 'photo'}.jpg`;
}

/**
 * Prepares a file for the upload: photos are scaled to {@link MAX_SIDE} and
 * saved as JPEG, which also drops their metadata like the location. PDFs and
 * small PNG / WebP files stay as they are; files the browser cannot decode
 * are sent unchanged and checked by the server.
 */
export async function prepareUpload(file: File): Promise<File> {
  if (!file.type.startsWith('image/')) return file;
  if (
    (file.type === 'image/png' || file.type === 'image/webp') &&
    file.size < KEEP_BELOW
  ) {
    return file;
  }

  const jpeg = await toJpeg(file, (width, height) => ({
    area: { x: 0, y: 0, width, height },
    target: fitInto(width, height),
  }));
  return jpeg ?? file;
}

/** side of the car pictures, enough for the largest avatar on a sharp screen */
export const PICTURE_SIDE = 512;

/**
 * @return the largest centered square of the image
 */
export function centerSquare(width: number, height: number) {
  const side = Math.min(width, height);
  return {
    x: Math.floor((width - side) / 2),
    y: Math.floor((height - side) / 2),
    width: side,
    height: side,
  };
}

/**
 * The centered square of a photo, scaled to {@link PICTURE_SIDE} as JPEG, for
 * the picture of a car
 *
 * @return the original, if the browser cannot decode the image
 */
export async function squarePicture(file: File): Promise<File> {
  const jpeg = await toJpeg(file, (width, height) => {
    const area = centerSquare(width, height);
    const side = Math.min(area.width, PICTURE_SIDE);
    return { area, target: { width: side, height: side } };
  });
  return jpeg ?? file;
}

interface Area {
  x: number;
  y: number;
  width: number;
  height: number;
}

/**
 * Draws an area of the image in a target size and saves it as JPEG, which
 * also drops the metadata like the location
 *
 * @param layout area and target size from the size of the decoded image
 * @return undefined, if the browser cannot decode the image
 */
async function toJpeg(
  file: File,
  layout: (
    width: number,
    height: number,
  ) => { area: Area; target: { width: number; height: number } },
): Promise<File | undefined> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    return undefined;
  }

  try {
    const { area, target } = layout(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = target.width;
    canvas.height = target.height;
    const context = canvas.getContext('2d');
    if (!context) return undefined;
    // transparent areas of PNGs become white instead of black
    context.fillStyle = '#fff';
    context.fillRect(0, 0, target.width, target.height);
    context.drawImage(
      bitmap,
      area.x,
      area.y,
      area.width,
      area.height,
      0,
      0,
      target.width,
      target.height,
    );

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY),
    );
    if (!blob) return undefined;
    return new File([blob], asJpegName(file.name), { type: 'image/jpeg' });
  } finally {
    bitmap.close();
  }
}
