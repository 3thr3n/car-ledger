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

  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' });
  } catch {
    return file;
  }

  try {
    const { width, height } = fitInto(bitmap.width, bitmap.height);
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) return file;
    // transparent areas of PNGs become white instead of black
    context.fillStyle = '#fff';
    context.fillRect(0, 0, width, height);
    context.drawImage(bitmap, 0, 0, width, height);

    const blob = await new Promise<Blob | null>((resolve) =>
      canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY),
    );
    if (!blob) return file;
    return new File([blob], asJpegName(file.name), { type: 'image/jpeg' });
  } finally {
    bitmap.close();
  }
}
