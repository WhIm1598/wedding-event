/** Studio logo limit, same as the backend's StudioLogo.MAX_BYTES */
export const MAX_LOGO_BYTES = 204_800;

const decodedBytes = (dataUrl: string) => Math.ceil((dataUrl.length - dataUrl.indexOf(',') - 1) * 0.75);

/**
 * Shrinks an image to at most `maxSide` px on its longest side (never upscales) and returns a PNG data URL,
 * or a JPEG one when the PNG is over 200 KB. PNG/JPEG only: the contract PDF can't draw WebP.
 */
export async function resizeToDataUrl(file: File, maxSide = 256): Promise<string> {
  if (!['image/png', 'image/jpeg'].includes(file.type)) throw new Error('Chọn file ảnh PNG hoặc JPEG');
  const url = URL.createObjectURL(file);
  try {
    const img = await new Promise<HTMLImageElement>((resolve, reject) => {
      const el = new Image();
      el.onload = () => resolve(el);
      el.onerror = () => reject(new Error('Không đọc được ảnh'));
      el.src = url;
    });
    const scale = Math.min(1, maxSide / Math.max(img.naturalWidth, img.naturalHeight));
    const canvas = document.createElement('canvas');
    canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
    canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
    canvas.getContext('2d')!.drawImage(img, 0, 0, canvas.width, canvas.height);
    const png = canvas.toDataURL('image/png');
    if (decodedBytes(png) <= MAX_LOGO_BYTES) return png;
    const jpeg = canvas.toDataURL('image/jpeg', 0.85);
    if (decodedBytes(jpeg) <= MAX_LOGO_BYTES) return jpeg;
    throw new Error('Ảnh quá lớn, chọn ảnh khác');
  } finally {
    URL.revokeObjectURL(url);
  }
}
