# Vision Aids

A small eyewear store for learning advanced Next.js with a Java backend.

The catalog renders 60 fictional frames from PostgreSQL through a Spring REST API
and a Next.js Server Component. Filter by eyeglasses, sunglasses, reading glasses,
or sports glasses using URL query parameters, for example `/glasses?cat=sunglasses`.
Select a frame to open `/glasses/[id]` with the complete gallery, frame details,
and placeholder description. Java supplies details through `/api/v1/products/{id}`;
lookups use UUIDs rather than slugs. The main image transitions between the two views
in supported browsers, with reduced-motion preferences respected.
The detail page links to `/checkout?productId=<id>` for a guest checkout form and
order summary. This first step supports one frame, delivery in Germany, and free
demo shipping. “Check details” sends the form through a Next.js Server Action to
`POST /api/v1/checkout/orders`. Java validates the contact/address fields and saves
a `PENDING_PAYMENT` order with the catalog name/price as a fixed snapshot. A
server-issued HttpOnly guest cookie allows the same browser to restore its saved
details on reload; retries update the same pending order rather than creating
duplicates. Java stores only a hash of the token and rejects access after 24 hours.
Pending orders contain personal data in the local database; automatic deletion of
expired rows is not implemented yet. No payment is taken. Accounts, lens
configuration, and Stripe are not implemented yet.
The storefront uses nuqs for URL filters: glasses type (`cat`), comma-separated
brands and shapes (`brand`, `shape`), and inclusive EUR price bounds (`minPrice`,
`maxPrice`). For example, `/glasses?cat=sunglasses&shape=Round,Oval&maxPrice=100`.
Checkbox choices match any selected value; different filter groups combine.
Next.js forwards filter parameters to Java, where JPA applies them in the database.
The Java endpoint `/api/v1/products/filters` supplies type labels and the full
catalog's distinct brand and shape choices, even when a selection has no results.
To add a filter, extend the Java controller and database predicates, the nuqs
parsers in `frontend/src/lib/catalog-filters.ts`, and its control in
`frontend/src/app/glasses/product-filters.tsx`. Next.js does not filter products.
Products support ordered images with a main thumbnail and independent rotation
selections. Catalog cards show only the main image, with a placeholder when it is
missing or unavailable. All 60 demo frames share five sets of local sample images;
see [image sources and selection rules](docs/product-images.md).

## Structure

```text
backend/     Java 21, Spring Boot 4.0.8, Maven, Spring MVC, JPA, Flyway
frontend/    Next.js 16.4.2, React 19, TypeScript, Tailwind CSS, ESLint
docs/        Architecture decisions and API contract
compose.yaml PostgreSQL 17 and both applications
```

## Run everything with Docker

Requires Docker Engine/Desktop with Docker Compose v2, plus network access for
the first build. Java, Maven, and Node are supplied by the container images.
On macOS, install and start Docker Desktop first. The images support both Intel
and Apple Silicon; no platform override or local Java/Node/PostgreSQL installation
is needed for this route. Use a fresh clone, then run from the repository root:

```bash
docker compose up --build
```

Open http://localhost:3000. The backend is at http://localhost:8080.
The backend waits for PostgreSQL; the frontend may briefly show an unavailable
message while Spring starts. Reload after the backend is ready.

The first startup creates the database and runs the seven tracked Flyway migrations
automatically. They create the schema and seed all 60 fictional products and their
240 image records. The 20 AI-generated image files are tracked in
`frontend/public/images/products/` and included in the frontend container; no image
download or database dump import is needed. Product IDs are stable across fresh
installations. No `.env` file is required for the Docker setup.
V7 adds the orders table without inserting any customer data.

Ports 3000, 8080, and 5432 must be available. If an existing container still serves
older code after pulling changes, run `docker compose up -d --build` to rebuild
and recreate it.

```bash
curl --fail http://localhost:8080/actuator/health
curl --fail http://localhost:8080/api/v1/products
curl --fail 'http://localhost:8080/api/v1/products?cat=sunglasses'
```

Stop with `docker compose down`. Database data persists in a named volume.
Compose is configured for local development with local database credentials.

To remove the local database and recreate the demo data, run the following from
the repository root. **This deletes all local database changes:**

```bash
docker compose down --volumes
docker compose up --build
```

### Optional database snapshots

The migrations are the source of the demo data. Use a dump only to transfer manual
database changes that are not represented by migrations. These commands use the
PostgreSQL tools inside Docker, so they also work on macOS without installing
`pg_dump` or `psql` locally.

Export a snapshot while the database is running:

```bash
docker compose exec -T db pg_dump -U vision_aids -d vision_aids --no-owner --no-acl > vision-aids.sql
```

The dump includes the schema, data, and Flyway migration history. Image files stay
in the repository; the database stores only their paths. The snapshot filename
above is ignored by Git because it may include local data. Share a reviewed
snapshot separately when another developer needs those exact changes.

To import a snapshot, stop any locally running backend too, then start only
PostgreSQL and wait until it reports healthy in `docker compose ps`.
**The drop/create commands replace the entire local database:**

```bash
docker compose stop frontend backend
docker compose up -d --wait db
docker compose exec -T db dropdb -U vision_aids --if-exists vision_aids
docker compose exec -T db createdb -U vision_aids vision_aids
docker compose exec -T db psql -U vision_aids -d vision_aids -v ON_ERROR_STOP=1 --single-transaction < vision-aids.sql
docker compose up -d --build
```

Restore a full dump into an empty database, before starting Spring. Do not import
it on top of the seeded database. The backend uses the restored Flyway history to
apply only migrations newer than the snapshot.

## Run with hot reload

Requires Node.js 26.12.1, npm, JDK 21, Maven 3.9+, and Docker for PostgreSQL.
If you manage Node with nvm, run these commands from the repository root:

```bash
nvm install
nvm use
```

The `.nvmrc`, frontend package engines, Node type definitions, and frontend
Docker stages target Node 26. Next.js 16.4.2 is the stable npm release verified
on 22 October 2026; its declared Node requirement (`>=20.9.0`) includes Node 26.

Start the database from the repository root:

```bash
docker compose up -d db
```

In a second terminal:

```bash
cd backend
mvn spring-boot:run
```

In a third terminal:

```bash
cd frontend
npm ci
cp .env.example .env.local
npm run dev
```

`frontend/.env.local` can override `BACKEND_URL` (default `http://localhost:8080`).
Spring accepts `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `PORT`; its defaults
match the database in Compose. Set these in your shell or IDE run configuration.

## Checks

```bash
cd frontend
npm run lint
npm test
npm run typecheck
npm run build
```

```bash
cd backend
mvn verify
```

The controller tests run without a database. The backend Docker build also runs
them. With PostgreSQL running, `CATALOG_DB_TESTS=true mvn verify` also runs catalog
and checkout database tests. Catalog fixtures roll back; checkout tests remove
their own committed fixtures after testing concurrent writes.
A complete smoke check needs PostgreSQL and both running applications:
the API should return 60 frames (15 per type), and the storefront should show
their names, types, and prices. Unsupported `cat` values return HTTP 400.
No mock catalog is substituted when the backend is unavailable.

Database image constraints have a separate SQL check. After applying the Flyway
migrations to a disposable PostgreSQL database, run
`psql -v ON_ERROR_STOP=1 -f backend/src/test/sql/product_images.sql` from the
repository root with connection settings for that database. It rolls back its
test data and checks main-thumbnail uniqueness, thumbnail replacement, independent
rotation selections, and image deletion.

## Starter choice

Spring Initializr's published dependency catalog has no Next.js starter. This
backend follows the official Spring REST service starter, with Web MVC,
Validation, Data JPA, Flyway, PostgreSQL, and Actuator added. The frontend uses
the standard Next.js App Router structure with TypeScript, Tailwind, and ESLint.

The scaffolds were assembled locally from those official conventions because
this workspace could not download the Initializr ZIP or run create-next-app.
Spring Boot 4.0.8 matches the official REST guide used as the baseline.
Next.js and eslint-config-next are pinned together at 16.4.2, replacing the
initial cached 16.2.6 baseline after checking the stable npm release and the
October security update. React and React DOM remain aligned at 19.2.7.

- [Spring Initializr dependency catalog](https://github.com/spring-io/start.spring.io/blob/main/start-site/src/main/resources/application.yml)
- [Spring REST service starter](https://spring.io/guides/gs/rest-service/)
- [Next.js installation guide](https://nextjs.org/docs/app/getting-started/installation)
- [Next.js stable package metadata](https://registry.npmjs.org/next/latest)
- [Next.js October security update](https://nextjs.org/blog/nextjs-security-update-october-14-2026)
- [Architecture and next steps](docs/architecture.md)
- [Initial API contract](docs/openapi.yaml)
