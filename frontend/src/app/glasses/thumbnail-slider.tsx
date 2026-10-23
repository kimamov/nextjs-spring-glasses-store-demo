"use client";

import { getImageProps } from "next/image";
import { useEffect, useRef } from "react";
import type { ProductImageDto } from "@/lib/catalog";

export default function ThumbnailSlider({ images, sizes, duration, timeout }: {
  images: ProductImageDto[];
  sizes: string;
  duration: number;
  timeout?: number;
}) {
  const container = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const element = container.current!;
    const motion = window.matchMedia("(prefers-reduced-motion: reduce)");
    const fadeDuration = Number.isFinite(duration) ? Math.max(0, duration) : 500;
    const fadeTimeout = timeout && Number.isFinite(timeout) ? Math.max(0, timeout) : 2500;
    let timer: ReturnType<typeof setTimeout>;
    let animation: Animation | undefined;
    let pending: HTMLImageElement | undefined;
    let index = 0;
    let generation = 0;
    const failed = new Set<string>();

    function stop() {
      generation++;
      clearTimeout(timer);
      animation?.cancel();
      if (pending) pending.onload = pending.onerror = null;
      element.replaceChildren();
      index = 0;
    }

    function advance() {
      const attempt = generation;
      index = (index + 1) % images.length;
      const image = images[index];
      if (failed.has(image.url)) {
        if (failed.size < images.length) timer = setTimeout(advance, fadeTimeout);
        return;
      }
      const next = new window.Image();
      pending = next;
      next.alt = "";
      next.className = "absolute inset-0 h-full w-full bg-white object-contain";
      next.style.opacity = "0";
      next.loading = "lazy";
      next.onload = () => {
        if (attempt !== generation) return;
        animation = next.animate([{ opacity: 0 }, { opacity: 1 }], { duration: fadeDuration, fill: "forwards" });
        animation.onfinish = () => {
          if (attempt !== generation) return;
          for (const child of Array.from(element.children)) if (child !== next) child.remove();
          timer = setTimeout(advance, Math.max(0, fadeTimeout - fadeDuration));
        };
      };
      next.onerror = () => {
        if (attempt !== generation) return;
        failed.add(image.url);
        next.remove();
        timer = setTimeout(advance, fadeTimeout);
      };
      const { props } = getImageProps({ src: image.url, alt: "", fill: true, sizes });
      next.sizes = sizes;
      next.srcset = props.srcSet ?? "";
      next.src = props.src;
      element.append(next);
    }

    function restart() {
      stop();
      if (!document.hidden && !motion.matches) timer = setTimeout(advance, fadeTimeout);
    }
    restart();
    document.addEventListener("visibilitychange", restart);
    motion.addEventListener("change", restart);
    return () => {
      stop();
      document.removeEventListener("visibilitychange", restart);
      motion.removeEventListener("change", restart);
    };
  }, [images, sizes, duration]);

  return <div ref={container} aria-hidden="true" className="pointer-events-none absolute inset-0" />;
}
