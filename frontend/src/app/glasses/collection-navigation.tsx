"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useLayoutEffect } from "react";

export function RestoreCollectionScroll() {
  useLayoutEffect(() => {
    const scrollY = window.history.state?.collectionScrollY;
    if (typeof scrollY === "number") window.scrollTo(0, scrollY);
  }, []);
  return null;
}

export function BackToCollection() {
  const router = useRouter();

  useEffect(() => {
    const destination = sessionStorage.getItem("collection-navigation");
    sessionStorage.removeItem("collection-navigation");
    if (destination === window.location.pathname) {
      window.history.replaceState({ ...window.history.state, fromCollection: true }, "");
    }
  }, []);

  return (
    <Link
      href="/glasses"
      className="mb-8 inline-block text-sm font-semibold text-brand"
      onNavigate={(event) => {
        if (window.history.state?.fromCollection) {
          event.preventDefault();
          router.back();
        }
      }}
    >
      ← Back to the collection
    </Link>
  );
}
