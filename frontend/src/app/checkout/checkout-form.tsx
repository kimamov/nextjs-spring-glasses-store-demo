"use client";

import { useActionState, useState } from "react";
import { Button } from "@/components/button";
import { Input } from "@/components/input";
import Link from "next/link";
import type { CheckoutFormState } from "@/lib/checkout";
import { saveCheckout } from "./actions";

export function CheckoutForm({ productId, initialState }: { productId: string; initialState: CheckoutFormState }) {
  // REVIEW: The server restores only the order belonging to this browser's cookie.
  // This keeps an ordinary page reload from discarding a saved address.
  const [state, action, pending] = useActionState<CheckoutFormState, FormData>(saveCheckout.bind(null, productId), initialState);
  const [edited, setEdited] = useState(false);
  const paid = state.order?.status === "PAID";

  return (
    <form action={action} aria-busy={pending} onChange={() => setEdited(true)} onSubmit={() => setEdited(false)} className="rounded-panel border border-divider bg-surface p-6 sm:p-8">
      <h2 className="text-2xl font-semibold">Your details</h2>
      <p className="mt-3 text-sm text-muted">Delivery within Germany. All fields are required unless marked optional.</p>
      <fieldset disabled={pending || paid} className="mt-6 grid gap-5 sm:grid-cols-2">
        <legend className="sr-only">Contact and delivery address</legend>
        <Input
          name="firstName" label="First name" autoComplete="given-name" maxLength={100} required
          defaultValue={state.values?.firstName ?? ""} error={state.errors?.firstName}
        />
        <Input
          name="lastName" label="Last name" autoComplete="family-name" maxLength={100} required
          defaultValue={state.values?.lastName ?? ""} error={state.errors?.lastName}
        />
        <Input
          name="email" label="Email address" type="email" autoComplete="email" maxLength={254} required
          className="sm:col-span-2" defaultValue={state.values?.email ?? ""} error={state.errors?.email}
        />
        <Input
          name="street" label="Street and house number" autoComplete="address-line1" maxLength={200} required
          className="sm:col-span-2" defaultValue={state.values?.street ?? ""} error={state.errors?.street}
        />
        <Input
          name="addressLine2" label="Apartment, suite, etc. (optional)" autoComplete="address-line2" maxLength={200}
          className="sm:col-span-2" defaultValue={state.values?.addressLine2 ?? ""} error={state.errors?.addressLine2}
        />
        <Input
          name="postalCode" label="Postal code" autoComplete="postal-code" maxLength={5} required
          inputMode="numeric" pattern="[0-9]{5}" defaultValue={state.values?.postalCode ?? ""} error={state.errors?.postalCode}
        />
        <Input
          name="city" label="City" autoComplete="address-level2" maxLength={100} required
          defaultValue={state.values?.city ?? ""} error={state.errors?.city}
        />
        <label className="flex flex-col gap-2 text-sm sm:col-span-2">
          Country
          <select name="country" autoComplete="country" defaultValue="DE" aria-invalid={Boolean(state.errors?.country) || undefined} aria-describedby={state.errors?.country ? "country-error" : undefined} className="min-h-12 rounded-lg border border-divider px-3 focus-visible:outline-2 focus-visible:outline-brand">
            <option value="DE">Germany</option>
          </select>
          {state.errors?.country && <span id="country-error" className="text-red-700">{state.errors.country}</span>}
        </label>
      </fieldset>
      <p className="mt-6 text-sm text-muted">Demo checkout. Your delivery details are saved here; payment continues on Stripe’s sandbox page. Use test card details only.</p>
      <Button type="submit" disabled={pending || paid} className="mt-6 w-full sm:w-auto">{paid ? "Payment confirmed" : pending ? "Opening payment…" : "Continue to payment"}</Button>
      {state.message && <p role="alert" className="mt-4 text-sm">{state.message}</p>}
      {state.order && !edited && (
        <p role="status" className="mt-4 text-sm text-brand">
          {paid ? "Payment confirmed" : "Pending order saved"} for {state.order.productName}. Total: {new Intl.NumberFormat("de-DE", { style: "currency", currency: state.order.currency }).format(state.order.total)}.
          {paid && <Link href={`/checkout/return?productId=${productId}`} className="ml-2 underline">View confirmation</Link>}
        </p>
      )}
    </form>
  );
}
