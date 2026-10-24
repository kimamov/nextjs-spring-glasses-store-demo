import Link from "next/link";
import { notFound } from "next/navigation";
import { getProduct, ProductNotFoundError } from "@/lib/catalog";
import { getPendingOrder } from "@/lib/checkout-server";
import { ProductThumbnail } from "../glasses/product-thumbnail";
import { RefreshCatalog } from "../glasses/refresh-catalog";
import { CheckoutForm } from "./checkout-form";

export default async function CheckoutPage({ searchParams }: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { productId } = await searchParams;
  if (typeof productId !== "string") notFound();
  let product;
  let order;
  try {
    product = await getProduct(productId);
    order = await getPendingOrder(product.id);
  } catch (error) {
    if (error instanceof ProductNotFoundError) notFound();
    // REVIEW: A failed response-schema check can contain the submitted address.
    // Log a generic failure instead of serializing that error or the request.
    console.error("Unable to load checkout");
    return (
      <section role="status" className="rounded-panel border border-divider bg-surface p-8">
        <h1 className="text-2xl font-semibold">Checkout is temporarily unavailable</h1>
        <p className="mt-3 text-muted">Please try again in a moment.</p>
        <RefreshCatalog />
      </section>
    );
  }

  // REVIEW: Once an order exists, show its agreed price snapshot, even if the
  // catalog price changed. The eventual Stripe session will use this same amount.
  const total = new Intl.NumberFormat("de-DE", { style: "currency", currency: order?.currency ?? product.currency }).format(order?.total ?? product.price);
  return (
    <>
      <Link href={`/glasses/${product.id}`} className="mb-8 inline-block text-sm font-semibold text-brand">← Back to the frame</Link>
      <h1 className="mb-8 text-display font-semibold">Checkout</h1>
      <div className="grid items-start gap-8 lg:grid-cols-[minmax(0,1fr)_20rem]">
        <CheckoutForm productId={product.id} initialState={order ? { order, values: { ...order.details, addressLine2: order.details.addressLine2 ?? "" } } : {}} />
        <aside aria-labelledby="order-summary" className="rounded-panel border border-divider bg-surface p-6">
          <h2 id="order-summary" className="text-xl font-semibold">Order summary</h2>
          <ProductThumbnail loading="eager" image={product.mainThumbnail ?? product.images[0] ?? null} className="mt-6 aspect-3/2" sizes="(min-width: 1024px) 272px, 100vw" />
          <p className="mt-5 text-xs uppercase tracking-eyebrow text-muted">{product.brand}</p>
          <h3 className="mt-2 text-2xl font-semibold">{order?.productName ?? product.name}</h3>
          <p className="mt-2 text-sm text-muted">{product.shape} · {product.color}</p>
          <dl className="mt-6 space-y-3 text-sm">
            <div className="flex justify-between gap-4"><dt>Frame × 1</dt><dd>{total}</dd></div>
            <div className="flex justify-between gap-4"><dt>Demo shipping</dt><dd>Free</dd></div>
            <div className="flex justify-between gap-4 border-t border-divider pt-3 text-lg font-semibold"><dt>Total</dt><dd>{total}</dd></div>
          </dl>
        </aside>
      </div>
    </>
  );
}
