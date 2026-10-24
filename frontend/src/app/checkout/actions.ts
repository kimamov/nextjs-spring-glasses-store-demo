"use server";

import * as v from "valibot";
import { cookies } from "next/headers";
import { revalidatePath } from "next/cache";
import { checkoutCookieName, checkoutFields, CheckoutProblemSchema, CheckoutOrderSchema, type CheckoutFormState } from "@/lib/checkout";

export async function saveCheckout(
  productId: string,
  _previousState: CheckoutFormState,
  formData: FormData,
): Promise<CheckoutFormState> {
  const values = Object.fromEntries(checkoutFields.map((name) => {
    const value = formData.get(name);
    return [name, typeof value === "string" ? value.trim() : ""];
  }));

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
    if (response.status === 409) return { values, message: "This checkout can no longer be changed. Start a new checkout." };
    if (!response.ok) throw new Error(`Checkout save failed: ${response.status}`);
    // REVIEW: A successful response means the database transaction committed.
    // It means "pending order saved", never "payment received".
    const order = v.parse(CheckoutOrderSchema, await response.json());
    // REVIEW: Refresh the server-rendered summary as well as the form result.
    // A price changed since the page opened must not leave two different totals.
    revalidatePath("/checkout");
    return { values, order };
  } catch {
    return { values, message: "We couldn’t confirm your checkout was saved. Please try again; a retry won’t create a duplicate." };
  }
}
