"use client";

import Image from "next/image";
import { lazy, Suspense, useMemo, useState, type ReactNode } from "react";
import type { ProductImageDto } from "@/lib/catalog";
import { ProductThumbnail } from "./product-thumbnail";

const ThumbnailSlider = lazy(() => import("./thumbnail-slider"));
const sizes = "(min-width: 1152px) 278px, (min-width: 1024px) calc((100vw - 318px) / 3), (min-width: 640px) calc((100vw - 236px) / 2), calc(100vw - 106px)";

export function ThumbnailCard({
  image,
  thumbnails,
  loading = "lazy",
  duration = 500,
  children,
}: {
  image: ProductImageDto | null;
  thumbnails: ProductImageDto[];
  // shortcut: eager loading ignores viewport width; consider matchMedia after hydration if mobile measurements justify it.
  loading?: "eager" | "lazy";
  duration?: number;
  children: ReactNode;
}) {
  const [hovered, setHovered] = useState(false);
  const [focused, setFocused] = useState(false);
  const [failed, setFailed] = useState(false);
  const images = useMemo(() => [image, ...thumbnails].filter(
    (candidate, index, all): candidate is ProductImageDto =>
      candidate !== null && all.findIndex((entry) => entry?.url === candidate.url) === index,
  ), [image, thumbnails]);
  const canRotate = images.length > 1;
  const active = canRotate && (hovered || focused);

  return (
    <li
      tabIndex={canRotate ? 0 : undefined}
      className="rounded-panel border border-divider bg-surface p-7 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-brand"
      onPointerEnter={(event) => {
        if (event.pointerType === "mouse" && !window.matchMedia("(prefers-reduced-motion: reduce)").matches) setHovered(true);
      }}
      onPointerLeave={() => setHovered(false)}
      onFocus={() => {
        if (!window.matchMedia("(prefers-reduced-motion: reduce)").matches) setFocused(true);
      }}
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setFocused(false);
      }}
    >
      <div className="relative h-36 overflow-hidden rounded-xl bg-white">
        {image && !failed ? (
          <Image
            src={image.url}
            alt={image.altText}
            fill
            sizes={sizes}
            loading={loading}
            className="object-contain"
            onError={() => setFailed(true)}
          />
        ) : <ProductThumbnail image={null} />}
        {active && (
          <Suspense fallback={null}>
            <ThumbnailSlider images={images} sizes={sizes} duration={duration} />
          </Suspense>
        )}
      </div>
      {children}
    </li>
  );
}
