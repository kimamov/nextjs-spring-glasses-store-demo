import * as v from "valibot";

export const ProductImageDtoSchema = v.object({
  url: v.string(),
  altText: v.string(),
  width: v.pipe(v.number(), v.integer(), v.minValue(1)),
  height: v.pipe(v.number(), v.integer(), v.minValue(1)),
});

export const ProductSummaryDtoSchema = v.object({
  id: v.string(),
  slug: v.string(),
  name: v.string(),
  brand: v.string(),
  type: v.string(),
  shape: v.string(),
  color: v.string(),
  price: v.pipe(v.number(), v.finite(), v.minValue(0)),
  currency: v.string(),
  mainThumbnail: v.nullable(ProductImageDtoSchema),
  rotatingThumbnails: v.array(ProductImageDtoSchema),
});

export type ProductImageDto = v.InferOutput<typeof ProductImageDtoSchema>;
export type ProductSummaryDto = v.InferOutput<typeof ProductSummaryDtoSchema>;
