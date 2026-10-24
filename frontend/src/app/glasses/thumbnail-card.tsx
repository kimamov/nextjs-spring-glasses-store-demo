"use client";

import Image from "next/image";
import Link from "next/link";
import { lazy, Suspense, useMemo, useState, ViewTransition, type ReactNode } from "react";
import type { ProductImageDto } from "@/lib/catalog";
import { ProductThumbnail } from "./product-thumbnail";

const ThumbnailSlider = lazy(() => import("./thumbnail-slider"));
const sizes = "(min-width: 1152px) 278px, (min-width: 1024px) calc((100vw - 318px) / 3), (min-width: 640px) calc((100vw - 236px) / 2), calc(100vw - 106px)";

export function ThumbnailCard({
  productId,
  image,
  thumbnails,
  loading = "lazy",
  duration = 500,
  children,
}: {
  productId: string;
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
      className="rounded-panel border border-divider bg-surface"
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
      <Link
        href={`/glasses/${productId}`}
        className="block h-full rounded-panel p-7"
        onNavigate={() => {
          sessionStorage.setItem("collection-navigation", `/glasses/${productId}`);
          window.history.replaceState({ ...window.history.state, collectionScrollY: window.scrollY }, "");
        }}
      >
        <ViewTransition name={`product-image-${productId}`} default="none" share="product-image">
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
        </ViewTransition>
        {children}
      </Link>
    </li>
  );
}
