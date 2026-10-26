import React, { useState, useEffect, useRef, useCallback } from 'react';
import { PictureAttachment } from '../types';
import { resolveImageSource } from '../utils/imageUtils';
import { X, ZoomIn, ZoomOut, RotateCcw, ChevronLeft, ChevronRight, Download, Move, ImageOff, ArrowLeft } from 'lucide-react';

interface ImageViewerModalProps {
  images: PictureAttachment[];
  initialIndex?: number;
  isOpen: boolean;
  onClose: () => void;
}

export const ImageViewerModal: React.FC<ImageViewerModalProps> = ({
  images,
  initialIndex = 0,
  isOpen,
  onClose
}) => {
  const [currentIndex, setCurrentIndex] = useState(initialIndex);
  const [scale, setScale] = useState(1);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [isDragging, setIsDragging] = useState(false);
  const dragStartRef = useRef({ x: 0, y: 0 });
  const containerRef = useRef<HTMLDivElement>(null);

  // Sync initial index when modal opens or images change
  useEffect(() => {
    if (isOpen) {
      setCurrentIndex(Math.max(0, Math.min(initialIndex, images.length - 1)));
      setScale(1);
      setPosition({ x: 0, y: 0 });
    }
  }, [isOpen, initialIndex, images.length]);

  const handleResetZoom = useCallback(() => {
    setScale(1);
    setPosition({ x: 0, y: 0 });
  }, []);

  const handleZoomIn = useCallback(() => {
    setScale(prev => Math.min(prev + 0.5, 5));
  }, []);

  const handleZoomOut = useCallback(() => {
    setScale(prev => {
      const next = Math.max(prev - 0.5, 1);
      if (next === 1) {
        setPosition({ x: 0, y: 0 });
      }
      return next;
    });
  }, []);

  const handlePrev = useCallback(() => {
    if (images.length <= 1) return;
    setCurrentIndex(prev => (prev > 0 ? prev - 1 : images.length - 1));
    handleResetZoom();
  }, [images.length, handleResetZoom]);

  const handleNext = useCallback(() => {
    if (images.length <= 1) return;
    setCurrentIndex(prev => (prev < images.length - 1 ? prev + 1 : 0));
    handleResetZoom();
  }, [images.length, handleResetZoom]);

  // Keyboard navigation
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      } else if (e.key === 'ArrowLeft') {
        handlePrev();
      } else if (e.key === 'ArrowRight') {
        handleNext();
      } else if (e.key === '+' || e.key === '=') {
        handleZoomIn();
      } else if (e.key === '-') {
        handleZoomOut();
      } else if (e.key === '0') {
        handleResetZoom();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose, handlePrev, handleNext, handleZoomIn, handleZoomOut, handleResetZoom]);

  // Mouse wheel zoom
  const handleWheel = (e: React.WheelEvent) => {
    e.preventDefault();
    if (e.deltaY < 0) {
      handleZoomIn();
    } else {
      handleZoomOut();
    }
  };

  // Dragging handlers (Mouse & Touch)
  const handleMouseDown = (e: React.MouseEvent) => {
    e.preventDefault();
    setIsDragging(true);
    dragStartRef.current = {
      x: e.clientX - position.x,
      y: e.clientY - position.y
    };
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDragging) return;
    e.preventDefault();
    setPosition({
      x: e.clientX - dragStartRef.current.x,
      y: e.clientY - dragStartRef.current.y
    });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
  };

  // Touch handlers for mobile
  const handleTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 1) {
      setIsDragging(true);
      dragStartRef.current = {
        x: e.touches[0].clientX - position.x,
        y: e.touches[0].clientY - position.y
      };
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isDragging || e.touches.length !== 1) return;
    setPosition({
      x: e.touches[0].clientX - dragStartRef.current.x,
      y: e.touches[0].clientY - dragStartRef.current.y
    });
  };

  const handleTouchEnd = () => {
    setIsDragging(false);
  };

  if (!isOpen || images.length === 0) return null;

  const currentImage = images[currentIndex] || images[0];
  const imgSrc = resolveImageSource(currentImage);

  return (
    <div
      id="fullscreen-image-viewer"
      className="fixed inset-0 z-50 bg-black/95 text-white flex flex-col select-none animate-in fade-in duration-200 w-full max-w-full h-full overflow-hidden"
      onWheel={handleWheel}
      onMouseUp={handleMouseUp}
    >
      {/* Top Header Controls */}
      <div className="flex items-center justify-between px-3 sm:px-6 py-2.5 sm:py-3 bg-black/60 backdrop-blur-md border-b border-white/10 shrink-0 z-10 w-full max-w-full overflow-x-hidden">
        <div className="flex items-center gap-2 sm:gap-3 min-w-0">
          <button
            type="button"
            onClick={onClose}
            className="p-1.5 -ml-1 sm:ml-0 rounded-xl text-white/80 hover:text-white hover:bg-white/10 transition-colors shrink-0"
            title="Back"
          >
            <ArrowLeft className="w-5 h-5" />
          </button>
          <span className="text-xs sm:text-sm font-bold font-mono tracking-wider text-slate-300 shrink-0">
            {currentIndex + 1} / {images.length}
          </span>
          <span className="text-xs text-slate-400 font-mono hidden sm:inline truncate max-w-xs">
            {currentImage?.filename || currentImage?.name || 'Garment Picture'}
          </span>
        </div>

        {/* Zoom & Reset Toolbar */}
        <div className="flex items-center gap-1.5 sm:gap-2">
          <span className="text-[11px] font-mono text-purple-300 bg-purple-950/60 px-2 py-0.5 rounded-md border border-purple-800/60 hidden xs:inline">
            {Math.round(scale * 100)}%
          </span>

          <button
            type="button"
            onClick={handleZoomOut}
            disabled={scale <= 1}
            title="Zoom Out (-)"
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white disabled:opacity-30 transition-colors"
          >
            <ZoomOut className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={handleZoomIn}
            disabled={scale >= 5}
            title="Zoom In (+)"
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white disabled:opacity-30 transition-colors"
          >
            <ZoomIn className="w-4 h-4" />
          </button>

          <button
            type="button"
            onClick={handleResetZoom}
            title="Reset Zoom & Position"
            className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors"
          >
            <RotateCcw className="w-4 h-4" />
          </button>

          {imgSrc && (
            <a
              href={imgSrc}
              download={currentImage?.filename || currentImage?.name || 'garment-photo.jpg'}
              target="_blank"
              rel="noreferrer"
              title="Download photo"
              className="p-2 rounded-xl bg-white/10 hover:bg-white/20 text-white transition-colors"
            >
              <Download className="w-4 h-4" />
            </a>
          )}

          <div className="h-5 w-[1px] bg-white/20 mx-1" />

          <button
            type="button"
            onClick={onClose}
            title="Close Fullscreen Viewer (Esc)"
            className="p-2 rounded-xl bg-white/20 hover:bg-rose-600 text-white transition-colors ml-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      </div>

      {/* Main Image Stage - Zoomable & Draggable */}
      <div
        ref={containerRef}
        className={`flex-1 relative overflow-hidden flex items-center justify-center p-4 ${
          scale > 1 || isDragging ? 'cursor-grab active:cursor-grabbing' : 'cursor-default'
        }`}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onTouchStart={handleTouchStart}
        onTouchMove={handleTouchMove}
        onTouchEnd={handleTouchEnd}
      >
        {imgSrc ? (
          <img
            src={imgSrc}
            alt={currentImage?.filename || currentImage?.name || 'Full preview'}
            referrerPolicy="no-referrer"
            draggable={false}
            style={{
              transform: `translate(${position.x}px, ${position.y}px) scale(${scale})`,
              transition: isDragging ? 'none' : 'transform 0.15s ease-out',
              maxHeight: '85vh',
              maxWidth: '90vw',
              objectFit: 'contain'
            }}
            className="select-none rounded-lg shadow-2xl pointer-events-none"
          />
        ) : (
          <div className="flex flex-col items-center justify-center p-6 text-center max-w-sm bg-white/5 border border-white/10 rounded-3xl backdrop-blur-md">
            <ImageOff className="w-12 h-12 text-amber-400/80 mb-3" />
            <h4 className="text-base font-bold text-white mb-1">Original Photo Unavailable</h4>
            <p className="text-xs text-slate-300 leading-relaxed">
              The external access link for this image has expired. Please upload a fresh photo from your device to save it permanently to local storage.
            </p>
          </div>
        )}

        {/* Previous Image Arrow */}
        {images.length > 1 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              handlePrev();
            }}
            title="Previous Image (Left Arrow)"
            className="absolute left-3 sm:left-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 hover:bg-white/20 text-white border border-white/20 backdrop-blur-md transition-transform active:scale-90"
          >
            <ChevronLeft className="w-6 h-6" />
          </button>
        )}

        {/* Next Image Arrow */}
        {images.length > 1 && (
          <button
            type="button"
            onClick={(e) => {
              e.stopPropagation();
              handleNext();
            }}
            title="Next Image (Right Arrow)"
            className="absolute right-3 sm:right-6 top-1/2 -translate-y-1/2 p-3 rounded-full bg-black/60 hover:bg-white/20 text-white border border-white/20 backdrop-blur-md transition-transform active:scale-90"
          >
            <ChevronRight className="w-6 h-6" />
          </button>
        )}

        {/* Floating pan hint */}
        {scale > 1 && (
          <div className="absolute bottom-4 left-1/2 -translate-x-1/2 px-3 py-1 rounded-full bg-black/70 text-slate-300 text-[11px] font-mono border border-white/10 flex items-center gap-1.5 backdrop-blur-xs pointer-events-none">
            <Move className="w-3 h-3 text-purple-400" />
            <span>Drag image left / right / up / down</span>
          </div>
        )}
      </div>

      {/* Thumbnail Strip (if multiple images) */}
      {images.length > 1 && (
        <div className="p-3 bg-black/70 backdrop-blur-md border-t border-white/10 flex items-center justify-center gap-2 overflow-x-auto shrink-0 max-h-24">
          {images.map((img, idx) => {
            const thumbUrl = resolveImageSource(img);
            return (
              <button
                key={img.id || idx}
                type="button"
                onClick={() => {
                  setCurrentIndex(idx);
                  handleResetZoom();
                }}
                className={`relative w-12 h-12 rounded-xl overflow-hidden border-2 transition-all shrink-0 ${
                  idx === currentIndex
                    ? 'border-purple-400 scale-105 shadow-md shadow-purple-500/30'
                    : 'border-white/20 opacity-50 hover:opacity-100'
                }`}
              >
                <img
                  src={thumbUrl}
                  alt={img.filename || img.name || `thumb ${idx}`}
                  referrerPolicy="no-referrer"
                  className="w-full h-full object-contain p-0.5"
                />
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};
