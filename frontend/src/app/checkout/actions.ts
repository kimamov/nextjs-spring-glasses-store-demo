"use server";

import * as v from "valibot";
import { checkoutFields, CheckoutProblemSchema, CheckoutSummarySchema, type CheckoutFormState } from "@/lib/checkout";

export async function validateCheckout(
  productId: string,
  _previousState: CheckoutFormState,
  formData: FormData,
): Promise<CheckoutFormState> {
  const values = Object.fromEntries(checkoutFields.map((name) => {
    const value = formData.get(name);
    return [name, typeof value === "string" ? value.trim() : ""];
  }));

  try {
    const response = await fetch(new URL("/api/v1/checkout/validate", process.env.BACKEND_URL ?? "http://localhost:8080"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
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
    if (!response.ok) throw new Error(`Checkout validation failed: ${response.status}`);
    return { values, summary: v.parse(CheckoutSummarySchema, await response.json()) };
  } catch {
    return { values, message: "We couldn’t check your details right now. Please try again." };
  }
}
