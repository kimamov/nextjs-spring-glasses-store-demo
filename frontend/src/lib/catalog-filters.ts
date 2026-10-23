import { createParser, parseAsArrayOf, parseAsString, type inferParserType } from "nuqs/server";

export const parsePrice = createParser({
  parse(value) {
    if (!/^(?:\d+(?:\.\d{1,2})?|\.\d{1,2})$/.test(value)) return null;
    const price = Number(value);
    return Number.isFinite(price) && price <= Number.MAX_SAFE_INTEGER / 100 ? price : null;
  },
  serialize: String,
});

export const catalogFilterParsers = {
  cat: parseAsString,
  brand: parseAsArrayOf(parseAsString).withDefault([]),
  shape: parseAsArrayOf(parseAsString).withDefault([]),
  minPrice: parsePrice,
  maxPrice: parsePrice,
};

export type CatalogFilters = inferParserType<typeof catalogFilterParsers>;

export function getPriceRangeError(min: number | null, max: number | null) {
  return min !== null && max !== null && min > max
    ? "Minimum price must not exceed maximum price."
    : null;
}

export type CatalogSearchParams = Record<string, string | string[] | undefined>;

export function catalogQuery(params: CatalogSearchParams) {
  const query = new URLSearchParams();
  for (const key of Object.keys(catalogFilterParsers)) {
    const value = params[key];
    if (value === undefined) continue;
    for (const entry of Array.isArray(value) ? value : [value]) query.append(key, entry);
  }
  return query;
}
