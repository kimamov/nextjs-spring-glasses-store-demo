"use server";

import * as v from "valibot";
import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { checkoutCookieName, checkoutFields, CheckoutProblemSchema, CheckoutOrderSchema, CheckoutSessionSchema, type CheckoutFormState } from "@/lib/checkout";
import { getCheckoutOrder } from "@/lib/checkout-server";

export async function saveCheckout(
  productId: string,
  _previousState: CheckoutFormState,
  formData: FormData,
): Promise<CheckoutFormState> {
  const values = Object.fromEntries(checkoutFields.map((name) => {
    const value = formData.get(name);
    return [name, typeof value === "string" ? value.trim() : ""];
  }));
  let destination: string;
  let savedOrder: CheckoutFormState["order"];

  try {
    // REVIEW: Never accept an order ID or guest token from the form. An attacker
    // can edit those fields. Next.js reads the server-issued HttpOnly cookie instead.
    const cookieStore = await cookies();
    const token = cookieStore.get(checkoutCookieName)?.value;
    if (!token) return { values, message: "Refresh this page to start a guest checkout." };
    const response = await fetch(new URL("/api/v1/checkout/orders", process.env.BACKEND_URL ?? "http://localhost:8080"), {
      method: "POST",
      headers: { "Content-Type": "application/json", "X-Checkout-Token": token },
      body: JSON.stringify({ productId, ...values }),
      cache: "no-store",
      signal: AbortSignal.timeout(5000),
    });
    if (response.status === 400) {
      const problem = v.safeParse(CheckoutProblemSchema, await response.json().catch(() => null));
      return { values, errors: problem.success ? problem.output.errors : undefined, message: "Check your details and try again." };
    }
    if (response.status === 404) {
      return { values, message: "This frame is no longer available. Please choose another frame." };
    }
    if (response.status === 410) {
      // REVIEW: Clearing an expired identity lets the next page request create
      // a new one. Match the cookie path when deleting it or the old cookie survives.
      cookieStore.set(checkoutCookieName, "", { path: "/checkout", maxAge: 0 });
      return { values, message: "This checkout has expired. Refresh the page to start again." };
    }
    if (response.status === 409) {
      const current = await getCheckoutOrder(productId);
      return { values, order: current ?? undefined, message: current?.status === "PAID" ? "Payment already confirmed." : "This checkout can no longer be changed." };
    }
    if (!response.ok) throw new Error(`Checkout save failed: ${response.status}`);
    // REVIEW: A successful response means the database transaction committed.
    // It means "pending order saved", never "payment received".
    const order = v.parse(CheckoutOrderSchema, await response.json());
    savedOrder = order;
    // REVIEW: Refresh the server-rendered summary as well as the form result.
    // A price changed since the page opened must not leave two different totals.
    revalidatePath("/checkout");
    const sessionUrl = new URL("/api/v1/checkout/sessions", process.env.BACKEND_URL ?? "http://localhost:8080");
    sessionUrl.searchParams.set("productId", productId);
    const session = await fetch(sessionUrl, {
      method: "POST",
      headers: { "X-Checkout-Token": token },
      cache: "no-store",
      // REVIEW: Session creation may include one network retry. The stable
      // backend idempotency key also protects a retry after this timeout.
      signal: AbortSignal.timeout(20000),
    });
    if (session.status === 409) {
      destination = `/checkout/return?productId=${encodeURIComponent(productId)}`;
    } else if (session.status === 410) {
      cookieStore.set(checkoutCookieName, "", { path: "/checkout", maxAge: 0 });
      return { values, message: "This checkout has expired. Refresh the page to start again." };
    } else if (!session.ok) {
      return { values, order, message: "Payment is temporarily unavailable. Your checkout is saved; please try again." };
    } else {
      destination = v.parse(CheckoutSessionSchema, await session.json()).url;
    }
  } catch {
    return { values, order: savedOrder, message: "We couldn’t open payment. Please try again; a retry won’t create a duplicate." };
  }
  // REVIEW: redirect throws a Next.js control-flow exception. Keep it outside the
  // catch above, otherwise a successful redirect is mistaken for a payment failure.
  redirect(destination);
}
