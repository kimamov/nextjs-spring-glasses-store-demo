import { Button } from "@/components/button";

export default function ProductNotFound() {
  return (
    <section>
      <h1 className="text-3xl font-semibold">Frame not found</h1>
      <p className="mt-4 text-muted">This frame is no longer available or the link is incorrect.</p>
      <Button href="/glasses" className="mt-6">Browse the collection</Button>
    </section>
  );
}
