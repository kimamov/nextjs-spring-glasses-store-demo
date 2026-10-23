import "server-only";
import { catalogQuery, type CatalogSearchParams } from "./catalog-filters";
import * as v from "valibot";
import { ProductSummaryDtoSchema, type ProductSummaryDto } from "./catalog-schemas";
export type { ProductImageDto, ProductSummaryDto } from "./catalog-schemas";

export type ProductFilterOptionsDto = {
  types: { value: string; label: string }[];
  brands: string[];
  shapes: string[];
};

export class CatalogFilterError extends Error {}

export async function getProducts(params: CatalogSearchParams = {}): Promise<ProductSummaryDto[]> {
  const json: unknown = await catalogRequest(`/api/v1/products?${catalogQuery(params)}`);
  return v.parse(v.array(ProductSummaryDtoSchema), json);
}

export function getProductFilterOptions(): Promise<ProductFilterOptionsDto> {
  return catalogRequest("/api/v1/products/filters");
}

async function catalogRequest<T>(path: string): Promise<T> {
  const baseUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  const response = await fetch(new URL(path, baseUrl), {
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });

  if (response.status === 400) {
    const problem = await response.json();
    throw new CatalogFilterError(typeof problem.detail === "string" ? problem.detail : "Invalid filters. Clear the filters and try again.");
  }
  if (!response.ok) {
    throw new Error(`Catalog request failed: ${response.status}`);
  }

  return response.json() as Promise<T>;
}
