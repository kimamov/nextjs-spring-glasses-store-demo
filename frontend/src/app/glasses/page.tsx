import { Suspense } from "react";
import { NuqsAdapter } from "nuqs/adapters/next/app";
import { CatalogFilterError, getProducts, getProductFilterOptions } from "@/lib/catalog";
import { ProductFilters } from "./product-filters";
import { RefreshCatalog } from "./refresh-catalog";
import { ThumbnailCard } from "./thumbnail-card";
import { RestoreCollectionScroll } from "./collection-navigation";

async function Catalog({ params }: { params: Record<string, string | string[] | undefined> }) {
  const [productResponse, filterResponse] = await Promise.allSettled([
    getProducts(params),
    getProductFilterOptions(),
  ]);
  if (filterResponse.status === "rejected" ||
    (productResponse.status === "rejected" && !(productResponse.reason instanceof CatalogFilterError))) {
    console.error("Unable to load catalog", productResponse, filterResponse);
    return (
      <section role="status" className="rounded-panel border border-divider bg-surface p-8">
        <h2 className="text-xl font-semibold">The collection is temporarily unavailable</h2>
        <p className="mt-3 text-muted">Please try again in a moment.</p>
        <RefreshCatalog />
      </section>
    );
  }

  const options = filterResponse.value;
  const filterError = productResponse.status === "rejected" ? productResponse.reason.message : null;
  const products = productResponse.status === "fulfilled" ? productResponse.value : [];

  return (
    <div className="flex flex-col gap-2 md:flex-row md:gap-4">
      <RestoreCollectionScroll />
      <aside className="md:w-80 md:shrink-0">
        <ProductFilters options={options} />
      </aside>
      <div className="min-w-0 flex-1">
        {filterError ? (
          <p role="status" className="py-12 text-muted">{filterError}</p>
        ) : products.length === 0 ? (
          <p role="status" className="py-12 text-muted">No glasses match these filters. Adjust your selections or clear the filters.</p>
        ) : (
          <>
            <p role="status" className="mb-6 text-sm text-muted">
              {products.length} {products.length === 1 ? "frame" : "frames"} found
            </p>
            <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {products.map((product, index) => (
                <ThumbnailCard
                  key={product.id}
                  productId={product.id}
                  image={product.mainThumbnail}
                  thumbnails={product.rotatingThumbnails}
                  loading={index < 3 ? "eager" : "lazy"}
                >
                  <p className="mt-6 text-xs uppercase tracking-eyebrow text-muted">{product.brand}</p>
                  <h3 className="mt-2 text-2xl font-semibold">{product.name}</h3>
                  <p className="mt-2 text-sm text-brand">
                    {options.types.find((type) => type.value === product.type)?.label ?? product.type}
                  </p>
                  <p className="mt-2 text-sm text-muted">{product.shape} · {product.color}</p>
                  <p className="mt-5 font-medium">
                    {new Intl.NumberFormat("de-DE", { style: "currency", currency: product.currency }).format(product.price)}
                    <span className="ml-2 text-xs font-normal text-muted">frame only</span>
                  </p>
                </ThumbnailCard>
              ))}
            </ul>
          </>
        )}
      </div>
    </div>
  );
}

export default async function GlassesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;

  return (
    <>
      <section className="mb-16 max-w-2xl">
        <p className="mb-5 text-sm font-semibold uppercase tracking-eyebrow text-brand">A fresh perspective</p>
        <h1 className="text-display font-semibold sm:text-display-lg">Find your everyday frame.</h1>
        <p className="mt-6 text-lg leading-relaxed text-muted">Thoughtful shapes. Easy colors. Meet a small collection made for the way you see the world.</p>
      </section>
      <section aria-labelledby="collection-heading">
        <h2 id="collection-heading" className="mb-6 text-2xl font-semibold">The collection</h2>
        <NuqsAdapter>
          <Suspense
            fallback={
              <p role="status" className="py-12 text-muted">Loading the collection…</p>
            }
          >
            <Catalog params={params} />
          </Suspense>
        </NuqsAdapter>
      </section>
    </>
  );
}
