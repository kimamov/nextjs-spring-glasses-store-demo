"use client";

import { useEffect, useRef, useState, useTransition } from "react";
import { useQueryStates } from "nuqs";
import { Button } from "@/components/button";
import { FilterGroup } from "@/components/filter-group";
import { catalogFilterParsers, getPriceRangeError, parsePrice, type CatalogFilters } from "@/lib/catalog-filters";
import type { ProductFilterOptionsDto } from "@/lib/catalog";

export function ProductFilters({ options }: { options: ProductFilterOptionsDto }) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [isMobile, setIsMobile] = useState(false);
  const [isPending, startTransition] = useTransition();
  const [filters, setFilters] = useQueryStates(catalogFilterParsers, {
    shallow: false,
    history: "push",
    startTransition,
  });
  const [resetCount, setResetCount] = useState(0);

  useEffect(() => {
    const media = window.matchMedia("(width < 48rem)");
    function syncLayout() {
      const dialog = dialogRef.current!;
      dialog.close();
      if (!media.matches) dialog.open = true;
      setIsMobile(media.matches);
    }
    syncLayout();
    media.addEventListener("change", syncLayout);
    return () => media.removeEventListener("change", syncLayout);
  }, []);

  return (
    <>
      <Button
        variant="secondary"
        className="mb-6 md:hidden"
        aria-haspopup="dialog"
        onClick={() => dialogRef.current?.showModal()}
      >
        Filters
      </Button>
      <dialog
        ref={dialogRef}
        role={isMobile ? "dialog" : "complementary"}
        aria-label="Glasses filters"
        className="fixed inset-y-0 left-0 m-0 h-dvh max-h-none w-3/4 max-w-none overflow-y-auto border-0 bg-surface p-4 text-ink backdrop:bg-black/40 md:static md:h-auto md:w-full md:overflow-visible md:bg-transparent md:p-0"
        onClick={(event) => {
          if (event.target !== event.currentTarget) return;
          const bounds = event.currentTarget.getBoundingClientRect();
          if (event.clientX < bounds.left || event.clientX > bounds.right ||
            event.clientY < bounds.top || event.clientY > bounds.bottom) {
            event.currentTarget.close();
          }
        }}
      >
        <div className="mb-4 flex items-center justify-between gap-2 md:hidden">
          <h3 className="text-xl font-semibold">Filters</h3>
          <Button variant="secondary" onClick={() => dialogRef.current?.close()}>Close</Button>
        </div>
        <form
          aria-label="Filter glasses"
          aria-busy={isPending}
          noValidate
          className="mb-8 rounded-panel border border-divider bg-surface p-6"
          onSubmit={(event) => event.preventDefault()}
          onReset={() => {
            setResetCount((count) => count + 1);
            void setFilters(null);
          }}
        >
          <div className="space-y-6">
            <FilterGroup
              type="radio"
              name="cat"
              label="Glasses type"
              options={[{ value: "", label: "All glasses" }, ...options.types]}
              value={filters.cat ?? ""}
              onChange={(cat) => void setFilters({ cat: cat || null })}
            />
            <FilterGroup
              type="checkbox"
              name="brand"
              label="Brand"
              options={options.brands.map((brand) => ({ value: brand, label: brand }))}
              value={filters.brand}
              onChange={(brand) => void setFilters({ brand })}
            />
            <FilterGroup
              type="checkbox"
              name="shape"
              label="Frame shape"
              options={options.shapes.map((shape) => ({ value: shape, label: shape }))}
              value={filters.shape}
              onChange={(shape) => void setFilters({ shape })}
            />
            <PriceRange
              key={`${filters.minPrice}:${filters.maxPrice}:${resetCount}`}
              value={filters}
              onChange={(prices) => void setFilters(prices)}
            />
          </div>
          <div className="mt-6 flex flex-wrap items-center gap-4">
            <Button type="reset" variant="secondary">Clear filters</Button>
            <p role="status" className="text-sm text-muted">{isPending ? "Updating results…" : "Filters update results automatically."}</p>
          </div>
        </form>
      </dialog>
    </>
  );
}

type PriceBounds = Pick<CatalogFilters, "minPrice" | "maxPrice">;

function PriceRange({ value, onChange }: { value: PriceBounds; onChange: (prices: PriceBounds) => void }) {
  const [error, setError] = useState<string | null>(null);

  function applyPrices(form: HTMLFormElement) {
    const minInput = form.elements.namedItem("minPrice") as HTMLInputElement;
    const maxInput = form.elements.namedItem("maxPrice") as HTMLInputElement;
    const minPrice = minInput.value === "" ? null : parsePrice.parse(minInput.value);
    const maxPrice = maxInput.value === "" ? null : parsePrice.parse(maxInput.value);
    if (!minInput.validity.valid || !maxInput.validity.valid ||
      (minInput.value !== "" && minPrice === null) || (maxInput.value !== "" && maxPrice === null)) {
      setError("Enter non-negative prices with at most two decimal places.");
      return;
    }
    const rangeError = getPriceRangeError(minPrice, maxPrice);
    setError(rangeError);
    if (!rangeError && (minPrice !== value.minPrice || maxPrice !== value.maxPrice)) {
      onChange({ minPrice, maxPrice });
    }
  }

  return (
    <fieldset>
      <legend className="mb-3 font-semibold">Price (EUR)</legend>
      <div className="flex flex-col gap-4">
        {(["minPrice", "maxPrice"] as const).map((name) => (
          <label key={name} className="flex flex-col gap-2 text-sm">
            {name === "minPrice" ? "Minimum price" : "Maximum price"}
            <input
              name={name}
              type="number"
              min="0"
              step="0.01"
              inputMode="decimal"
              defaultValue={value[name] ?? ""}
              aria-invalid={error ? true : undefined}
              aria-describedby={error ? "price-error" : undefined}
              className="min-h-11 w-full rounded-lg border border-divider px-3 focus-visible:outline-2 focus-visible:outline-brand"
              onChange={() => setError(null)}
              onBlur={(event) => applyPrices(event.currentTarget.form!)}
              onKeyDown={(event) => {
                if (event.key !== "Enter") return;
                event.preventDefault();
                applyPrices(event.currentTarget.form!);
              }}
            />
          </label>
        ))}
      </div>
      {error && <p id="price-error" role="alert" className="mt-3 text-sm">{error}</p>}
      <p className="mt-3 text-xs text-muted">Price changes apply on leaving a field or pressing Enter.</p>
    </fieldset>
  );
}
