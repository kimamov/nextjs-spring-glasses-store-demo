"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { Button } from "@/components/button";

export function PaymentStatus({ productId, productName, total, currency, paid, cancelled }: {
  productId: string; productName: string; total: number; currency: string; paid: boolean; cancelled: boolean;
}) {
  const router = useRouter();
  useEffect(() => {
    if (paid || cancelled) return;
    // REVIEW: The browser can return before Stripe's webhook arrives. Refresh the
    // owner's database status briefly; visiting this page never marks an order paid.
    const interval = setInterval(() => router.refresh(), 2000);
    const timeout = setTimeout(() => clearInterval(interval), 30000);
    return () => { clearInterval(interval); clearTimeout(timeout); };
  }, [paid, cancelled, router]);

  return (
    <section className="mx-auto max-w-xl rounded-panel border border-divider bg-surface p-8">
      <h1 className="text-3xl font-semibold">{paid ? "Payment confirmed" : cancelled ? "Checkout is still pending" : "Confirming your payment"}</h1>
      <p className="mt-4 text-muted" role="status">
        {paid ? "Your sandbox payment was confirmed. This is a demo order; no frame will be shipped."
          : cancelled ? "You returned from Stripe. Your details are saved, and you can resume payment."
          : "We’re waiting for payment confirmation. If you haven’t completed payment, you can resume checkout."}
      </p>
      <p className="mt-6 font-semibold">{productName} · {new Intl.NumberFormat("de-DE", { style: "currency", currency }).format(total)}</p>
      <div className="mt-8 flex flex-wrap gap-4">
        {!paid && <Button href={`/checkout?productId=${productId}`}>Resume checkout</Button>}
        {!paid && <Button variant="secondary" onClick={() => router.refresh()}>Refresh status</Button>}
        {paid && <Button href="/glasses">Back to the collection</Button>}
      </div>
    </section>
  );
}
