"use client";

import { useActionState, useState } from "react";
import { Button } from "@/components/button";
import { Input } from "@/components/input";
import type { CheckoutFormState } from "@/lib/checkout";
import { validateCheckout } from "./actions";

export function CheckoutForm({ productId }: { productId: string }) {
  const [state, action, pending] = useActionState<CheckoutFormState, FormData>(validateCheckout.bind(null, productId), {});
  const [edited, setEdited] = useState(false);

  return (
    <form action={action} aria-busy={pending} onChange={() => setEdited(true)} onSubmit={() => setEdited(false)} className="rounded-panel border border-divider bg-surface p-6 sm:p-8">
      <h2 className="text-2xl font-semibold">Your details</h2>
      <p className="mt-3 text-sm text-muted">Delivery within Germany. All fields are required unless marked optional.</p>
      <fieldset disabled={pending} className="mt-6 grid gap-5 sm:grid-cols-2">
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
      <p className="mt-6 text-sm text-muted">Demo checkout. Your details are checked but aren’t saved. No order or payment is created.</p>
      <Button type="submit" disabled={pending} className="mt-6 w-full sm:w-auto">{pending ? "Checking…" : "Check details"}</Button>
      {state.message && <p role="alert" className="mt-4 text-sm">{state.message}</p>}
      {state.summary && !edited && (
        <p role="status" className="mt-4 text-sm text-brand">
          Your details are valid. The current total for {state.summary.productName} is {new Intl.NumberFormat("de-DE", { style: "currency", currency: state.summary.currency }).format(state.summary.total)}.
          {" "}Payment is not available yet.
        </p>
      )}
    </form>
  );
}
