import { notFound } from "next/navigation";
import * as v from "valibot";
import { getCheckoutOrder } from "@/lib/checkout-server";
import { Button } from "@/components/button";
import { RefreshCatalog } from "../../glasses/refresh-catalog";
import { PaymentStatus } from "./payment-status";

export default async function CheckoutReturn({ searchParams }: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { productId, cancelled } = await searchParams;
  if (typeof productId !== "string" || !v.is(v.pipe(v.string(), v.uuid()), productId)) notFound();
  let order;
  try {
    order = await getCheckoutOrder(productId);
  } catch {
    return <section><h1 className="text-3xl font-semibold">Payment status is temporarily unavailable</h1><p className="mt-4 text-muted">Please check again before starting another payment.</p><RefreshCatalog /></section>;
  }
  if (!order) {
    return <section><h1 className="text-3xl font-semibold">No checkout found</h1><p className="mt-4 text-muted">This checkout is missing or your access has expired.</p><Button href="/glasses" className="mt-6">Browse the collection</Button></section>;
  }
  return <PaymentStatus productId={order.productId} productName={order.productName} total={order.total} currency={order.currency} paid={order.status === "PAID"} cancelled={cancelled === "1"} />;
}
