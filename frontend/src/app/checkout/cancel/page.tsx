import { notFound, redirect } from "next/navigation";

export default async function CheckoutCancel({ searchParams }: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const { productId } = await searchParams;
  if (typeof productId !== "string") notFound();
  // REVIEW: Returning from Stripe does not cancel an open payment session or
  // override a payment completed in another tab. The return page reads real status.
  redirect(`/checkout/return?productId=${encodeURIComponent(productId)}&cancelled=1`);
}
