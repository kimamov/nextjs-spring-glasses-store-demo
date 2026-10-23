import assert from "node:assert/strict";
import test from "node:test";
import * as v from "valibot";
import { ProductSummaryDtoSchema } from "./catalog-schemas.ts";

const image = { url: "/images/frame.png", altText: "Frame", width: 384, height: 204 };
const product = {
  id: "10000000-0000-0000-0000-000000000001",
  slug: "frame", name: "Frame", brand: "Northline", type: "eyeglasses",
  shape: "Round", color: "Black", price: 89, currency: "EUR",
  mainThumbnail: image, rotatingThumbnails: [image],
};
const productsSchema = v.array(ProductSummaryDtoSchema);

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
