# Architecture

## Request flow

```text
Browser → Next.js Server Component → Spring REST API → PostgreSQL
```

Next.js owns rendering and frontend interaction. Spring owns product data and,
as the project grows, pricing, validation, carts, inventory, and order creation.
The browser does not connect directly to the Java backend in this initial slice.
`BACKEND_URL` stays on the Next.js server; no public environment variable or CORS
configuration is necessary for this request flow.

The glasses page streams its catalog section through Suspense. Catalog requests are
explicitly uncached for now, with a five-second timeout and an honest unavailable
state. This avoids build-time database access and makes backend changes visible
while learning. Introduce catalog caching and tag invalidation as a later feature;
keep personal carts and sessions out of shared caches.

## Backend boundaries

The Java application uses packages by feature, starting with `catalog`.
The controller calls an application service, which reads from a Spring Data
repository. API records keep persistence entities out of HTTP responses.
The list endpoint returns `ProductSummaryDto` records with a nullable `mainThumbnail`
and ordered `rotatingThumbnails`. Selection happens in the backend, so the
frontend consumes the prepared image roles directly. Shared `ProductImageDto`
records contain only the URL, alt text, and source dimensions. `ProductDetailDto`
adds the full ordered image gallery to those fields and is returned by
`GET /api/v1/products/{id}`. Missing products return 404; malformed UUIDs return 400.
The storefront links catalog cards to `/glasses/[id]`, shows every image and
placeholder description text, and pairs the card image with the detail hero using
React View Transitions. Reduced-motion preferences disable the animation.
Flyway owns schema creation; Hibernate validates the schema at startup.
The first two migrations create the catalog and seed fictional demo products.
The third adds product types and expands the demo catalog to 60 frames.
The fourth adds ordered product images with alt text, one main thumbnail per
nonempty gallery, and independent flags for future thumbnail rotation. A deferred
database constraint checks the main selection at transaction commit, allowing
image lists and thumbnail selections to be replaced in a single transaction.
Images are fetched with products to avoid one extra query per product.
The fifth migration adds required positive source dimensions in pixels, backfilled
from the local sample files. The API includes these dimensions; responsive image
variants remain the responsibility of Next.js.
There is no automatic schema update or production catalog integration.

Catalog cards render only the selected main thumbnail, retaining a placeholder
when there is no image or it fails to load. No gallery, rotation UI, upload, or
image editing endpoint is implemented. The sample assets live in the storefront;
their URLs are relative to its origin. See [image provenance](product-images.md).

The list endpoint supports an optional `cat` product-type filter and returns
matching products ordered by name. Unsupported types return HTTP 400. Its optional
query predicate can be extended with additional filters without a generic filter
framework. Add bounded pagination before growing the dataset further.

The glasses page reads filters from its server-side `searchParams` prop. Next.js
links update `cat` while preserving other query parameters, triggering a fresh
server fetch without reloading the document. The URL remains the source of truth
for direct links and browser history. A Suspense boundary keyed by type shows
loading feedback when the selected category changes.

## API contract

`docs/openapi.yaml` documents the first endpoint. The frontend currently has a
small handwritten response type in `src/lib/catalog.ts`; it is not generated or
runtime validated. Introduce OpenAPI generation and contract checks when the API
grows, so Java and TypeScript cannot silently drift apart.

## Suggested next slices

1. Product details and frame variants, with metadata and route-level not-found UI.
2. Additional URL-driven filters, sorting, and bounded pagination.
3. Lens configuration with Java validation and server-calculated prices.
4. Anonymous cookie-based cart, Server Actions, and optimistic updates.
5. Simulated checkout with stock checks, transactions, and idempotency.
6. PostgreSQL integration tests with Testcontainers and a Playwright purchase flow.

Introduce a commerce-provider interface when a second provider or a migration
exercise gives it a concrete use. The initial structure does not depend on
Salesforce or Intershop accounts.
