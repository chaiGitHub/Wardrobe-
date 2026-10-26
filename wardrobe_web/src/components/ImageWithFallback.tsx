import React, { useState, useRef } from 'react';
import { Shirt, Camera, Upload, Loader2, ImageOff } from 'lucide-react';
import { uploadGarmentPhotoToStorage } from '../services/cloudStorageService';
import { dataService } from '../services/dataService';
import { isAttachmentExpired } from '../utils/imageUtils';
import { getCachedImageUrl } from '../services/localImageCache';

interface ImageWithFallbackProps {
  src?: string;
  alt: string;
  clothesId?: string;
  className?: string;
  fallbackIconClass?: string;
  onImageUpdated?: (newUrl: string) => void;
  aspectRatioClass?: string;
}

export const ImageWithFallback: React.FC<ImageWithFallbackProps> = ({
  src,
  alt,
  clothesId,
  className = 'w-full h-full object-contain',
  fallbackIconClass = 'w-10 h-10',
  onImageUpdated,
  aspectRatioClass
}) => {
  const [currentSrc, setCurrentSrc] = useState(src || '');
  const [hasError, setHasError] = useState(false);
  const [fallbackAttempt, setFallbackAttempt] = useState(0);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Sync with incoming src changes
  React.useEffect(() => {
    setCurrentSrc(src || '');
    setHasError(false);
    setFallbackAttempt(0);
  }, [src]);

  // Check if this clothes item originally had an expired attachment
  const garment = clothesId ? dataService.getClothes().find(c => c.id === clothesId) : null;
  const originalPics = garment?.['Pictures'] || [];
  const hadExpiredOriginal = originalPics.length > 0 && originalPics.some(p => isAttachmentExpired(p));

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !clothesId) return;

    setIsUploading(true);
    setUploadError(null);

    try {
      const picAttachment = await uploadGarmentPhotoToStorage(clothesId, file);
      const newUrl = picAttachment.url || picAttachment.presignedUrl || picAttachment.smThumbnailUrl || '';

      if (newUrl) {
        // Update the garment in dataService & Firebase Cloud Storage
        const currentGarment = dataService.getClothes().find(c => c.id === clothesId);
        if (currentGarment) {
          const updatedPictures = [picAttachment, ...(currentGarment['Pictures'] || [])];
          await dataService.updateGarment(clothesId, {
            'Pictures': updatedPictures
          });
        }

        setCurrentSrc(newUrl);
        setHasError(false);
        onImageUpdated?.(newUrl);
      }
    } catch (err: any) {
      console.error('Firebase Storage upload failed:', err);
      setUploadError(err?.message || 'Upload failed. Please try again.');
    } finally {
      setIsUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleImageError = () => {
    // 1. Try alternate path formats
    if (fallbackAttempt === 0 && currentSrc) {
      setFallbackAttempt(1);
      if (currentSrc.startsWith('/uploads/photos/')) {
        setCurrentSrc('/uploads/' + currentSrc.slice('/uploads/photos/'.length));
        return;
      } else if (currentSrc.startsWith('/uploads/')) {
        setCurrentSrc('/uploads/photos/' + currentSrc.slice('/uploads/'.length));
        return;
      }
    } else if (fallbackAttempt === 1 && garment?.Pictures?.[0]?.storagePath) {
      setFallbackAttempt(2);
      const sp = garment.Pictures[0].storagePath.replace(/^\/+/, '').replace(/^uploads\//, '');
      const altUrl = `/uploads/${sp}`;
      if (altUrl !== currentSrc) {
        setCurrentSrc(altUrl);
        return;
      }
    }

    // 2. Fallback to local IndexedDB blob storage
    const picObj = garment?.Pictures?.[0];
    const key = picObj?.storagePath || picObj?.filename || picObj?.name || picObj?.id || currentSrc;
    if (key && fallbackAttempt < 3) {
      setFallbackAttempt(3);
      getCachedImageUrl(key).then(idbUrl => {
        if (idbUrl && idbUrl !== currentSrc) {
          setCurrentSrc(idbUrl);
          setHasError(false);
          return;
        }
        setHasError(true);
      }).catch(() => setHasError(true));
      return;
    }

    setHasError(true);
  };

  // If no valid source is present or if an error occurred loading the image
  if (!currentSrc || hasError) {
    return (
      <div className={`relative flex flex-col items-center justify-center p-3 text-center bg-slate-50 border border-dashed border-slate-200 rounded-2xl w-full h-full transition-colors group/img-box ${aspectRatioClass || ''}`}>
        {hadExpiredOriginal ? (
          <ImageOff className={`${fallbackIconClass} stroke-[1.25] text-amber-500/80 mb-1`} />
        ) : hasError ? (
          <ImageOff className={`${fallbackIconClass} stroke-[1.25] text-slate-400 mb-1`} />
        ) : (
          <Shirt className={`${fallbackIconClass} stroke-[1.25] text-slate-300 mb-1`} />
        )}

        <span className="text-[11px] font-medium text-slate-700 line-clamp-1 text-center font-sans">
          {alt || 'Garment'}
        </span>

        <span className="text-[10px] text-slate-600 mt-0.5 mb-2 font-mono">
          {hadExpiredOriginal
            ? 'Original S3 link expired'
            : hasError
            ? 'Photo unavailable'
            : 'No photo uploaded'}
        </span>

        {clothesId && (
          <>
            <button
              type="button"
              disabled={isUploading}
              onClick={(e) => {
                e.stopPropagation();
                fileInputRef.current?.click();
              }}
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-xl bg-purple-600 hover:bg-purple-700 text-white font-semibold text-[10px] shadow-xs transition-all active:scale-95 disabled:opacity-50 cursor-pointer"
              title="Upload photo to local disk"
            >
              {isUploading ? (
                <>
                  <Loader2 className="w-3 h-3 animate-spin" />
                  <span>Saving to Disk...</span>
                </>
              ) : (
                <>
                  <Camera className="w-3 h-3" />
                  <span>{hadExpiredOriginal || hasError ? 'Upload Local Photo' : 'Add Photo'}</span>
                </>
              )}
            </button>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className="hidden"
              onClick={(e) => e.stopPropagation()}
              onChange={handleFileChange}
            />
          </>
        )}

        {uploadError && (
          <span className="text-[9px] text-rose-600 font-medium mt-1">
            {uploadError}
          </span>
        )}
      </div>
    );
  }

  return (
    <img
      src={currentSrc}
      alt={alt}
      className={className}
      referrerPolicy="no-referrer"
      onError={handleImageError}
    />
  );
};
