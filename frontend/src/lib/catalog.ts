import "server-only";

export const productTypes = [
  { value: "eyeglasses", label: "Eyeglasses" },
  { value: "sunglasses", label: "Sunglasses" },
  { value: "reading-glasses", label: "Reading glasses" },
  { value: "sports-glasses", label: "Sports glasses" },
] as const;

export type ProductImageDto = {
  url: string;
  altText: string;
  width: number;
  height: number;
};

export type ProductSummaryDto = {
  id: string;
  slug: string;
  name: string;
  brand: string;
  type: (typeof productTypes)[number]["value"];
  shape: string;
  color: string;
  price: number;
  currency: string;
  mainThumbnail: ProductImageDto | null;
  rotatingThumbnails: ProductImageDto[];
};

export async function getProducts(filters: { cat?: string } = {}): Promise<ProductSummaryDto[]> {
  const baseUrl = process.env.BACKEND_URL ?? "http://localhost:8080";
  const url = new URL("/api/v1/products", baseUrl);
  if (filters.cat !== undefined) {
    url.searchParams.set("cat", filters.cat);
  }
  const response = await fetch(url, {
    cache: "no-store",
    signal: AbortSignal.timeout(5000),
  });

  if (!response.ok) {
    throw new Error(`Catalog request failed: ${response.status}`);
  }

  return response.json() as Promise<ProductSummaryDto[]>;
}
