import { notFound } from "next/navigation";
import { ViewTransition } from "react";
import { getProduct, ProductNotFoundError } from "@/lib/catalog";
import { ProductThumbnail } from "../product-thumbnail";
import { RefreshCatalog } from "../refresh-catalog";
import { BackToCollection } from "../collection-navigation";

export default async function ProductPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  let product;
  try {
    product = await getProduct(id);
  } catch (error) {
    if (error instanceof ProductNotFoundError) notFound();
    console.error("Unable to load product", error);
    return (
      <section role="status" className="rounded-panel border border-divider bg-surface p-8">
        <h1 className="text-2xl font-semibold">This frame is temporarily unavailable</h1>
        <p className="mt-3 text-muted">Please try again in a moment.</p>
        <RefreshCatalog />
      </section>
    );
  }

  const mainImage = product.mainThumbnail ?? product.images[0] ?? null;
  const otherImages = product.images.filter((image) => image.url !== mainImage?.url);

  return (
    <>
      <BackToCollection />
      <article className="grid items-start gap-10 md:grid-cols-2">
        <section aria-label={`${product.name} images`} className="min-w-0 space-y-4">
          <ViewTransition name={`product-image-${product.id}`} default="none" share="product-image">
            <ProductThumbnail
              image={mainImage}
              className="aspect-3/2"
              sizes="(min-width: 1152px) 508px, (min-width: 768px) 50vw, 100vw"
              loading="eager"
            />
          </ViewTransition>
          {otherImages.length > 0 && (
            <ul className="grid grid-cols-2 gap-4">
              {otherImages.map((image, index) => (
                <li key={`${image.url}-${index}`}>
                  <ProductThumbnail image={image} className="aspect-3/2" sizes="(min-width: 768px) 25vw, 50vw" />
                </li>
              ))}
            </ul>
          )}
        </section>
        <div>
          <p className="text-sm uppercase tracking-eyebrow text-muted">{product.brand}</p>
          <h1 className="mt-3 text-display font-semibold">{product.name}</h1>
          <p className="mt-4 capitalize text-brand">{product.type.replaceAll("-", " ")}</p>
          <p className="mt-3 text-muted">{product.shape} · {product.color}</p>
          <p className="mt-6 text-2xl font-semibold">
            {new Intl.NumberFormat("de-DE", { style: "currency", currency: product.currency }).format(product.price)}
            <span className="ml-2 text-sm font-normal text-muted">frame only</span>
          </p>
          <h2 className="mt-8 text-xl font-semibold">About this frame</h2>
          <p className="mt-4 leading-relaxed text-muted">
            Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.
            Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.
          </p>
          <p className="mt-8 border-t border-divider pt-6 text-sm text-muted">Ordering will be available here soon.</p>
        </div>
      </article>
    </>
  );
}
