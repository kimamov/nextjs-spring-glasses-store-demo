import assert from "node:assert/strict";
import test from "node:test";
import * as v from "valibot";
import { CatalogProblemSchema, ProductDetailDtoSchema, ProductFilterOptionsDtoSchema, ProductSummaryDtoSchema } from "./catalog-schemas.ts";

const image = { url: "/images/frame.png", altText: "Frame", width: 384, height: 204 };
const product = {
  id: "10000000-0000-0000-0000-000000000001",
  slug: "frame", name: "Frame", brand: "Northline", type: "eyeglasses",
  shape: "Round", color: "Black", price: 89, currency: "EUR",
  mainThumbnail: image, rotatingThumbnails: [image],
};
const productsSchema = v.array(ProductSummaryDtoSchema);

test("product details require the complete gallery and validate every image", () => {
  const detail = { ...product, images: [image, { ...image, url: "/images/detail.png" }] };
  assert.deepEqual(v.parse(ProductDetailDtoSchema, detail), detail);
  assert.deepEqual(v.parse(ProductDetailDtoSchema, { ...product, mainThumbnail: null, images: [] }).images, []);
  for (const images of [undefined, null, "invalid", [{ ...image, width: 0 }]]) {
    assert.throws(() => v.parse(ProductDetailDtoSchema, { ...product, images }), v.ValiError);
  }
});

test("accepts product lists, relative image paths, and empty lists", () => {
  assert.deepEqual(v.parse(productsSchema, [product]), [product]);
  assert.deepEqual(v.parse(productsSchema, []), []);
});

test("accepts null main thumbnails, empty rotation, and zero price", () => {
  const valid = { ...product, mainThumbnail: null, rotatingThumbnails: [], price: 0 };
  assert.deepEqual(v.parse(productsSchema, [valid]), [valid]);
});

test("rejects negative, non-finite, and non-numeric prices", () => {
  for (const price of [-1, Infinity, -Infinity, NaN, "89", null]) {
    assert.throws(() => v.parse(productsSchema, [{ ...product, price }]), v.ValiError);
  }
});

test("requires positive integer dimensions in main and rotating thumbnails", () => {
  for (const dimension of ["width", "height"]) {
    for (const invalid of [0, -1, 1.5, Infinity, "384"]) {
      const malformed = { ...image, [dimension]: invalid };
      assert.throws(() => v.parse(productsSchema, [{ ...product, mainThumbnail: malformed }]), v.ValiError);
      assert.throws(() => v.parse(productsSchema, [{ ...product, rotatingThumbnails: [malformed] }]), v.ValiError);
    }
  }
});

test("rejects missing required nullable fields and non-list responses", () => {
  const missingThumbnail = { ...product };
  delete missingThumbnail.mainThumbnail;
  assert.throws(() => v.parse(productsSchema, [missingThumbnail]), v.ValiError);
  for (const invalid of [null, product, { products: [product] }]) {
    assert.throws(() => v.parse(productsSchema, invalid), v.ValiError);
  }
});

test("tolerates new backend fields and returns the parsed output without them", () => {
  assert.deepEqual(v.parse(productsSchema, [{ ...product, extra: true }]), [product]);
});

test("accepts filter options including an empty catalog", () => {
  const options = { types: [{ value: "eyeglasses", label: "Eyeglasses" }], brands: ["Northline"], shapes: ["Round"] };
  assert.deepEqual(v.parse(ProductFilterOptionsDtoSchema, options), options);
  const empty = { types: [], brands: [], shapes: [] };
  assert.deepEqual(v.parse(ProductFilterOptionsDtoSchema, empty), empty);
});

test("rejects malformed filter option groups and nested choices", () => {
  for (const invalid of [
    null,
    { types: [], brands: [] },
    { types: [{ value: "eyeglasses" }], brands: [], shapes: [] },
    { types: [{ value: 1, label: "Eyeglasses" }], brands: [], shapes: [] },
    { types: [], brands: "Northline", shapes: [] },
    { types: [], brands: [], shapes: [123] },
  ]) assert.throws(() => v.parse(ProductFilterOptionsDtoSchema, invalid), v.ValiError);
});

test("extracts usable problem details while tolerating extra backend fields", () => {
  assert.deepEqual(v.parse(CatalogProblemSchema, { status: 400, detail: " Invalid price. " }), { detail: "Invalid price." });
  assert.equal(v.safeParse(CatalogProblemSchema, {}).success, true);
});

test("malformed or blank problem details can use the error fallback", () => {
  for (const invalid of [null, [], "error", { detail: null }, { detail: 123 }, { detail: "" }, { detail: "  " }]) {
    const result = v.safeParse(CatalogProblemSchema, invalid);
    assert.equal(result.success ? result.output.detail : undefined, undefined);
  }
});
