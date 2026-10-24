import type { Metadata } from "next";
import Link from "next/link";
import "./globals.css";

export const metadata: Metadata = {
  title: "Vision Aids | Find your frame",
  description: "A small eyewear store built to explore Next.js and Java Spring Boot.",
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>
        <a
          href="#main-content"
          className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-50 focus:rounded-lg focus:bg-surface focus:px-6 focus:py-3 focus:text-brand"
        >
          Skip to content
        </a>
        <header className="border-b border-divider px-6 py-6 sm:px-12">
          <Link href="/" className="inline-flex min-h-11 items-center text-xl font-semibold tracking-tight">vision aids<span className="text-accent">.</span></Link>
        </header>
        <main id="main-content" tabIndex={-1} className="mx-auto max-w-6xl px-6 py-16 sm:px-12">{children}</main>
        <footer className="border-t border-divider px-6 py-8 text-center text-sm text-muted">
          A learning project. Fictional products. Sandbox payments only; no real purchases.
        </footer>
      </body>
    </html>
  );
}
