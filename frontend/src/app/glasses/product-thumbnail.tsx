"use client";

import Image from "next/image";
import { useState } from "react";
import type { ProductImageDto } from "@/lib/catalog";

export function ProductThumbnail({ image }: { image: ProductImageDto | null }) {
  return <Thumbnail key={image?.url} image={image} />;
}

function Thumbnail({ image }: { image: ProductImageDto | null }) {
  const [failed, setFailed] = useState(false);

  if (!image || failed) {
    return (
      <div role="img" aria-label="Product image unavailable" className="flex h-36 items-center justify-center rounded-xl bg-surface-soft text-6xl text-brand">
        <span aria-hidden="true">⌐◯-◯</span>
      </div>
    );
  }

  return (
    <div className="relative h-36 overflow-hidden rounded-xl bg-white">
      <Image
        src={image.url}
        alt={image.altText}
        fill
        sizes="(min-width: 1024px) 33vw, (min-width: 640px) 50vw, 100vw"
        className="object-contain"
        onError={() => setFailed(true)}
      />
    </div>
  );
}
