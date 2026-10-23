"use client";

import { useState, useTransition } from "react";
import { useQueryStates } from "nuqs";
import { Button } from "@/components/button";
import { FilterGroup } from "@/components/filter-group";
import { catalogFilterParsers, getPriceRangeError, parsePrice, type CatalogFilters } from "@/lib/catalog-filters";
import type { ProductFilterOptionsDto } from "@/lib/catalog";

export function ProductFilters({ options }: { options: ProductFilterOptionsDto }) {
  const [isPending, startTransition] = useTransition();
  const [filters, setFilters] = useQueryStates(catalogFilterParsers, {
    shallow: false,
    history: "push",
    startTransition,
  });
  const [resetCount, setResetCount] = useState(0);

  return (
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
      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
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
