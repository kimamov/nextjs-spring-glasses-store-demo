"use client";

import { Button } from "@/components/button";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <section className="py-16">
      <h1 className="text-3xl font-semibold">Something went wrong</h1>
      <p className="mt-4 text-muted">We couldn’t load this page.</p>
      <Button onClick={reset} className="mt-6">Try again</Button>
    </section>
  );
}
