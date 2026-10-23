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

export const ProductFilterOptionsDtoSchema = v.object({
  types: v.array(v.object({ value: v.string(), label: v.string() })),
  brands: v.array(v.string()),
  shapes: v.array(v.string()),
});

export const CatalogProblemSchema = v.object({
  detail: v.optional(v.pipe(v.string(), v.trim(), v.nonEmpty())),
});

export type ProductImageDto = v.InferOutput<typeof ProductImageDtoSchema>;
export type ProductSummaryDto = v.InferOutput<typeof ProductSummaryDtoSchema>;
export type ProductFilterOptionsDto = v.InferOutput<typeof ProductFilterOptionsDtoSchema>;
