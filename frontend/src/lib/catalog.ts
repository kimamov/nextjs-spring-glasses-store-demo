import "server-only";
import { catalogQuery, type CatalogSearchParams } from "./catalog-filters";
import * as v from "valibot";
import {
  CatalogProblemSchema,
  ProductFilterOptionsDtoSchema,
  ProductSummaryDtoSchema,
  type ProductFilterOptionsDto,
  type ProductSummaryDto,
} from "./catalog-schemas";
export type { ProductImageDto, ProductSummaryDto, ProductFilterOptionsDto } from "./catalog-schemas";

export class CatalogFilterError extends Error {}

export function getProducts(params: CatalogSearchParams = {}): Promise<ProductSummaryDto[]> {
  return catalogRequest(`/api/v1/products?${catalogQuery(params)}`, v.array(ProductSummaryDtoSchema));
}

export function getProductFilterOptions(): Promise<ProductFilterOptionsDto> {
  return catalogRequest("/api/v1/products/filters", ProductFilterOptionsDtoSchema);
}

async function catalogRequest<TSchema extends v.GenericSchema>(
  path: string,
  schema: TSchema,
): Promise<v.InferOutput<TSchema>> {
  const baseUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  const response = await fetch(new URL(path, baseUrl), {
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });

  if (response.status === 400) {
    const problem = v.safeParse(CatalogProblemSchema, await response.json().catch(() => null));
    throw new CatalogFilterError(problem.success && problem.output.detail
      ? problem.output.detail
      : "Invalid filters. Clear the filters and try again.");
  }
  if (!response.ok) {
    throw new Error(`Catalog request failed: ${response.status}`);
  }

  const json: unknown = await response.json();
  return v.parse(schema, json);
}
