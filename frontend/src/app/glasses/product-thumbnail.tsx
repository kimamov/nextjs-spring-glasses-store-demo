"use client";

import Image from "next/image";
import { useState } from "react";
import type { ProductImageDto } from "@/lib/catalog";

type ThumbnailProps = {
  image: ProductImageDto | null;
  className?: string;
  sizes?: string;
  loading?: "eager" | "lazy";
};

export function ProductThumbnail(props: ThumbnailProps) {
  return <Thumbnail key={props.image?.url} {...props} />;
}

function Thumbnail({
  image,
  className = "h-36",
  sizes = "(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw",
  loading = "lazy",
}: ThumbnailProps) {
  const [failed, setFailed] = useState(false);

  if (!image || failed) {
    return (
      <div role="img" aria-label="Product image unavailable" className={`flex items-center justify-center rounded-xl bg-surface-soft text-6xl text-brand ${className}`}>
        <span aria-hidden="true">⌐◯-◯</span>
      </div>
    );
  }

  return (
    <div className={`relative overflow-hidden rounded-xl bg-white ${className}`}>
      <Image
        src={image.url}
        alt={image.altText}
        fill
        sizes={sizes}
        loading={loading}
        className="object-contain"
        onError={() => setFailed(true)}
      />
    </div>
  );
}
