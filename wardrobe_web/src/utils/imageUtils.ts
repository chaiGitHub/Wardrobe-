/**
 * Helper to reliably resolve image source URLs from any picture attachment format:
 * Teable records, Firestore objects, local uploads, thumbnails, presigned URLs or data URLs.
 */
export function resolveImageSource(img: any): string {
  if (!img) return '';
  if (typeof img === 'string') return img;
  return (
    img.url ||
    img.originalUrl ||
    img.presignedUrl ||
    img.lgThumbnailUrl ||
    img.smThumbnailUrl ||
    img.thumbnails?.large?.url ||
    img.thumbnails?.small?.url ||
    img.dataUrl ||
    img.path ||
    ''
  );
}
