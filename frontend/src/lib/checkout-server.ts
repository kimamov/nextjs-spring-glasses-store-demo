import "server-only";

import { cookies } from "next/headers";
import * as v from "valibot";
import { checkoutCookieName, CheckoutOrderSchema } from "./checkout";

export async function getCheckoutOrder(productId: string) {
  const token = (await cookies()).get(checkoutCookieName)?.value;
  if (!token) return null;
  const url = new URL("/api/v1/checkout/orders/current", process.env.BACKEND_URL ?? "http://localhost:8080");
  url.searchParams.set("productId", productId);
  // REVIEW: Personal details travel only on the server connection. The random
  // token is not a URL parameter, a form field, or a browser-readable API response.
  const response = await fetch(url, {
    headers: { "X-Checkout-Token": token },
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });
  if (response.status === 404 || response.status === 410) return null;
  if (!response.ok) throw new Error(`Pending checkout unavailable: ${response.status}`);
  return v.parse(CheckoutOrderSchema, await response.json());
}
