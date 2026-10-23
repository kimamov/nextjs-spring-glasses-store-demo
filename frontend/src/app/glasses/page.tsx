import { Suspense } from "react";
import { Button } from "@/components/button";
import { getProducts, productTypes } from "@/lib/catalog";
import { RefreshCatalog } from "./refresh-catalog";
import { ProductThumbnail } from "./product-thumbnail";

async function Catalog({ cat }: { cat?: string }) {
  let products;
  try {
    products = await getProducts({ cat });
  } catch (error) {
    console.error("Unable to load catalog", error);
    return (
      <section role="status" className="rounded-panel border border-divider bg-surface p-8">
        <h2 className="text-xl font-semibold">The collection is temporarily unavailable</h2>
        <p className="mt-3 text-muted">Please try again in a moment.</p>
        <RefreshCatalog />
      </section>
    );
  }

  if (products.length === 0) {
    return (
      <p role="status" className="py-12 text-muted">
        No glasses match this type. Choose another type or select All glasses.
      </p>
    );
  }

  return (
    <>
      <p role="status" className="mb-6 text-sm text-muted">
        {products.length} {products.length === 1 ? "frame" : "frames"} found
      </p>
      <ul className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {products.map((product) => (
          <li key={product.id} className="rounded-panel border border-divider bg-surface p-7">
            <ProductThumbnail image={product.images.find((image) => image.mainThumbnail)} />
            <p className="mt-6 text-xs uppercase tracking-eyebrow text-muted">{product.brand}</p>
            <h3 className="mt-2 text-2xl font-semibold">{product.name}</h3>
            <p className="mt-2 text-sm text-brand">
              {productTypes.find((type) => type.value === product.type)?.label}
            </p>
            <p className="mt-2 text-sm text-muted">{product.shape} · {product.color}</p>
            <p className="mt-5 font-medium">
              {new Intl.NumberFormat("de-DE", { style: "currency", currency: product.currency }).format(product.price)}
              <span className="ml-2 text-xs font-normal text-muted">frame only</span>
            </p>
          </li>
        ))}
      </ul>
    </>
  );
}

export default async function GlassesPage({
  searchParams,
}: {
  searchParams: Promise<Record<string, string | string[] | undefined>>;
}) {
  const params = await searchParams;
  const cat = Array.isArray(params.cat) ? params.cat.join(",") : params.cat;
  const validCategory =
    cat === undefined || productTypes.some((type) => type.value === cat);

  return (
    <>
      <section className="mb-16 max-w-2xl">
        <p className="mb-5 text-sm font-semibold uppercase tracking-eyebrow text-brand">A fresh perspective</p>
        <h1 className="text-display font-semibold sm:text-display-lg">Find your everyday frame.</h1>
        <p className="mt-6 text-lg leading-relaxed text-muted">Thoughtful shapes. Easy colors. Meet a small collection made for the way you see the world.</p>
      </section>
      <section aria-labelledby="collection-heading">
        <h2 id="collection-heading" className="mb-6 text-2xl font-semibold">The collection</h2>
        <nav aria-label="Filter glasses by type" className="mb-8">
          <ul className="flex flex-wrap gap-3">
            {[{ value: "", label: "All glasses" }, ...productTypes].map((type) => {
              const query = { ...params };
              if (type.value) {
                query.cat = type.value;
              } else {
                delete query.cat;
              }
              const active = validCategory && (cat ?? "") === type.value;

              return (
                <li key={type.value}>
                  <Button
                    href={{ pathname: "/glasses", query }}
                    variant={active ? "primary" : "secondary"}
                    aria-current={active ? "page" : undefined}
                    scroll={false}
                    prefetch={false}
                  >
                    {type.label}
                  </Button>
                </li>
              );
            })}
          </ul>
        </nav>
        {validCategory ? (
          <Suspense
            key={cat ?? "all"}
            fallback={
              <p role="status" className="py-12 text-muted">Loading the collection…</p>
            }
          >
            <Catalog cat={cat} />
          </Suspense>
        ) : (
          <p role="status" className="py-12 text-muted">
            This glasses type is not supported. Choose a type above to browse the collection.
          </p>
        )}
      </section>
    </>
  );
}
