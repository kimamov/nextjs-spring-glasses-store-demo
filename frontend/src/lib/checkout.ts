import * as v from "valibot";

export const checkoutFields = [
  "firstName", "lastName", "email", "street", "addressLine2", "postalCode", "city", "country",
] as const;

// REVIEW: These names/settings are shared, but the token value is never exported
// to client components. It lives in an HttpOnly cookie and server-side requests.
export const checkoutCookieName = "guest-checkout";
export const checkoutCookieMaxAge = 60 * 60 * 24;
export const checkoutTokenPattern = /^[a-f0-9]{64}$/;

export const CheckoutSummarySchema = v.object({
  productId: v.pipe(v.string(), v.uuid()),
  productName: v.string(),
  total: v.pipe(v.number(), v.finite(), v.minValue(0)),
  currency: v.pipe(v.string(), v.regex(/^[A-Z]{3}$/)),
});

export const CheckoutProblemSchema = v.object({
  errors: v.optional(v.record(v.string(), v.string())),
});

export const CheckoutOrderSchema = v.object({
  ...CheckoutSummarySchema.entries,
  orderId: v.pipe(v.string(), v.uuid()),
  status: v.picklist(["PENDING_PAYMENT", "PAID"]),
  details: v.object({
    productId: v.pipe(v.string(), v.uuid()),
    firstName: v.string(),
    lastName: v.string(),
    email: v.string(),
    street: v.string(),
    addressLine2: v.nullable(v.string()),
    postalCode: v.string(),
    city: v.string(),
    country: v.literal("DE"),
  }),
});

// REVIEW: Never redirect to an arbitrary URL returned by an upstream response.
// This demo uses Stripe's hosted page, without custom payment domains.
export const CheckoutSessionSchema = v.object({
  url: v.pipe(v.string(), v.url(), v.regex(/^https:\/\/checkout\.stripe\.com\//)),
});

export type CheckoutFormState = {
  values?: Partial<Record<typeof checkoutFields[number], string>>;
  errors?: Record<string, string>;
  message?: string;
  order?: v.InferOutput<typeof CheckoutOrderSchema>;
};
