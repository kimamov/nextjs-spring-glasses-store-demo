import * as v from "valibot";

export const checkoutFields = [
  "firstName", "lastName", "email", "street", "addressLine2", "postalCode", "city", "country",
] as const;

export const CheckoutSummarySchema = v.object({
  productId: v.pipe(v.string(), v.uuid()),
  productName: v.string(),
  total: v.pipe(v.number(), v.finite(), v.minValue(0)),
  currency: v.pipe(v.string(), v.regex(/^[A-Z]{3}$/)),
});

export const CheckoutProblemSchema = v.object({
  errors: v.optional(v.record(v.string(), v.string())),
});

export type CheckoutFormState = {
  values?: Partial<Record<typeof checkoutFields[number], string>>;
  errors?: Record<string, string>;
  message?: string;
  summary?: v.InferOutput<typeof CheckoutSummarySchema>;
};
