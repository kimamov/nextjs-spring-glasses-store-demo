import assert from "node:assert/strict";
import test from "node:test";
import { createLoader } from "nuqs/server";
import { catalogFilterParsers, catalogQuery, getPriceRangeError } from "./catalog-filters.ts";

const loadCatalogFilters = createLoader(catalogFilterParsers);

test("only catalog filters are forwarded to Java", () => {
  assert.equal(catalogQuery({ utm_source: "demo", cat: undefined }).toString(), "");
  const params = { cat: "eyeglasses", brand: "Northline,Studio Frame", shape: "Round,Oval", minPrice: "49.99", maxPrice: "100" };
  assert.deepEqual(Object.fromEntries(catalogQuery(params)), params);
});

test("nuqs parses checkbox selections for client controls", () => {
  const filters = loadCatalogFilters("?cat=eyeglasses&brand=Northline,Studio%20Frame&shape=Round,Oval");
  assert.deepEqual(filters.brand, ["Northline", "Studio Frame"]);
  assert.deepEqual(filters.shape, ["Round", "Oval"]);
});

test("raw invalid values and repeated parameters reach Java unchanged for validation", () => {
  const query = catalogQuery({ brand: ["Northline", "Studio Frame"], minPrice: "invalid", maxPrice: "", cat: "unknown" });
  assert.deepEqual(query.getAll("brand"), ["Northline", "Studio Frame"]);
  assert.equal(query.get("minPrice"), "invalid");
  assert.equal(query.get("maxPrice"), "");
  assert.equal(query.get("cat"), "unknown");
});

test("strict parsing rejects invalid URL prices", () => {
  for (const value of ["-1", "NaN", "Infinity", "12oops", "1.234", "1e2", "", "9999999999999999999"]) {
    assert.throws(() => loadCatalogFilters({ minPrice: value }, { strict: true }));
  }
  assert.equal(loadCatalogFilters({ minPrice: ".50" }, { strict: true }).minPrice, 0.5);
});

test("reversed ranges have a useful validation error", () => {
  assert.match(getPriceRangeError(100, 50), /Minimum price/);
  assert.equal(getPriceRangeError(50, 50), null);
  assert.equal(getPriceRangeError(null, 50), null);
});
