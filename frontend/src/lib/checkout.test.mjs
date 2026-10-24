import assert from "node:assert/strict";
import test from "node:test";
import * as v from "valibot";
import { CheckoutOrderSchema, CheckoutProblemSchema, CheckoutSessionSchema, CheckoutSummarySchema } from "./checkout.ts";

const summary = { productId: "10000000-0000-0000-0000-000000000001", productName: "Mitte", total: 89, currency: "EUR" };

test("saved orders require an ID, pending status and complete owner details", () => {
  const order = { ...summary, orderId: "20000000-0000-0000-0000-000000000001", status: "PENDING_PAYMENT",
    details: { productId: summary.productId, firstName: "Ada", lastName: "Lovelace", email: "ada@example.com",
      street: "Example Street 1", addressLine2: null, postalCode: "10115", city: "Berlin", country: "DE" } };
  assert.deepEqual(v.parse(CheckoutOrderSchema, order), order);
  assert.equal(v.parse(CheckoutOrderSchema, { ...order, status: "PAID" }).status, "PAID");
  for (const invalid of [{ orderId: undefined }, { status: "REFUNDED" }, { details: undefined }]) {
    assert.throws(() => v.parse(CheckoutOrderSchema, { ...order, ...invalid }), v.ValiError);
  }
});

test("payment redirects accept only Stripe-hosted HTTPS checkout URLs", () => {
  assert.equal(v.parse(CheckoutSessionSchema, { url: "https://checkout.stripe.com/c/pay/cs_test" }).url, "https://checkout.stripe.com/c/pay/cs_test");
  for (const url of ["javascript:alert(1)", "http://checkout.stripe.com/pay", "https://checkout.stripe.com.evil.test/pay", "https://evil.test", "//checkout.stripe.com/pay"]) {
    assert.throws(() => v.parse(CheckoutSessionSchema, { url }), v.ValiError);
  }
});

test("checkout accepts validated summaries and rejects malformed prices, currencies and IDs", () => {
  assert.deepEqual(v.parse(CheckoutSummarySchema, summary), summary);
  for (const invalid of [{ total: -1 }, { total: "89" }, { total: Infinity }, { currency: "euro" }, { productId: "mitte" }]) {
    assert.throws(() => v.parse(CheckoutSummarySchema, { ...summary, ...invalid }), v.ValiError);
  }
});

test("checkout extracts field errors without forwarding unrelated response fields", () => {
  assert.deepEqual(v.parse(CheckoutProblemSchema, { errors: { email: "Enter a valid email address." }, status: 400 }),
    { errors: { email: "Enter a valid email address." } });
  for (const invalid of [null, { errors: "invalid" }, { errors: { email: 123 } }]) {
    assert.equal(v.safeParse(CheckoutProblemSchema, invalid).success, false);
  }
});
