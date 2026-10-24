import { Button } from "@/components/button";

export default function CheckoutNotFound() {
  return (
    <section>
      <h1 className="text-3xl font-semibold">Choose a frame to check out</h1>
      <p className="mt-4 text-muted">The selected frame is missing or no longer available.</p>
      <Button href="/glasses" className="mt-6">Browse the collection</Button>
    </section>
  );
}
