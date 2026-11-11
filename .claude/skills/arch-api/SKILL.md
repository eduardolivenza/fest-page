---
name: arch-api
description: FestPage API (apps/api) — Java/Spring Boot hexagonal architecture (one Gradle module, one package per layer), route map, request validation, error format and status codes, admin JWT auth, Turnstile, JPA persistence on the Prisma-era schema, Flyway migrations, and how API tests work. Use when adding or changing an endpoint, touching the database schema, or debugging API behaviour.
---

# FestPage — API (`apps/api`)

Java 25, Spring Boot 4 (Spring MVC, Spring Security, Spring Data JPA/Hibernate), Flyway, Gradle (Kotlin DSL, wrapper in `apps/api`). Versions: `apps/api/gradle/libs.versions.toml` (see `toolchain-versions`). Records everywhere except JPA entities; **no Lombok**.

## Hexagonal layout: one Gradle module, one package per layer

```
com.festdescans.api
  domain/                  records + business rules, pure Java (no Spring, no JPA)
  application/             use cases: port/in (interfaces), port/out (interfaces), service/ (plain Java)
  adapter/in/web/          REST controllers, request/response records, Bean Validation, errors, JWT security, CORS, OpenAPI
  adapter/out/persistence/ JPA entities + Spring Data repos implementing port/out, cuid ids, transactions
  adapter/out/turnstile/   CaptchaVerifier over the JDK HttpClient (plain Java)
  adapter/out/password/    PasswordVerifier: Argon2id PHC hashes with BouncyCastle (plain Java)
  FestPageApplication, UseCaseConfiguration (wires services to adapters), TurnstileProperties
src/main/resources         application.yml, db/migration (Flyway)
```

`HexagonalArchitectureTest` (ArchUnit) enforces the dependency rule: `domain` ← `application` ← adapters, adapters never depend on each other, and `domain`/`application` import no Spring, Jakarta, Hibernate or Jackson. Only the root package (composition root) sees everything. Application services have no Spring annotations; `UseCaseConfiguration` creates them as beans.

## Routes

| Prefix | Controller | Endpoints |
|---|---|---|
| `/api/products` | `product/ProductController` | `GET /` (`?category=&featured=true&page=&pageSize=` max 50, first image only), `GET /{idOrSlug}` |
| `/api/contacts` | `contact/ContactController` | `POST /` (Turnstile-protected) |
| `/api/orders` | `order/OrderController` | `POST /` (customer upsert by email + order priced with current product prices) |
| `/api/admin/auth` | `auth/AdminAuthController` | `POST /login` → `{ token, user }`, `GET /me` |
| `/api/admin/products` | `product/AdminProductController` | `GET /`, `GET /{id}`, `POST /`, `PUT /{id}` (partial; `sizes` replaces all), `DELETE /{id}` |
| `/api/admin/orders` | `order/AdminOrderController` | `GET /` (`?status=`), `PATCH /{id}/status` |
| `/api/admin/contacts` | `contact/AdminContactController` | `GET /` (`?read=`), `PATCH /{id}/read`, `DELETE /{id}` |
| `/health`, `/health/liveness`, `/health/readiness` | Spring Boot Actuator (`management.*` in `application.yml`, base path `/`) | overall status incl. DB / liveness (startup + liveness probes, never the DB) / readiness (DOWN during shutdown). Only `health` is exposed. |
| `/docs`, `/docs/json` | springdoc | Swagger UI / OpenAPI JSON (public) |

## Conventions

- **Validate every body** with a request record + Bean Validation (`@Valid`; create-only `@NotNull`s use group `Validation.OnCreate`). Errors → `400 { message: 'Invalid data', errors: { formErrors, fieldErrors } }` (same shape as the old Zod `flatten()`). Jackson is strict: `"2"` or `2.5` is not an integer (`JsonConfiguration`).
- **Errors are `{ message }`**, mapped centrally in `error/ApiExceptionHandler` from domain/application exceptions: 400 invalid input / unknown products, 401 no/invalid token or bad credentials (same message for unknown user and wrong password), 403 captcha failed, 404 `NotFoundException` / unknown route, 409 `ProductInUseException`, `SlugAlreadyUsedException`; 201 created, 204 deleted. Controllers don't catch exceptions.
- Business rules live in the domain records' factory/update methods (`Product.create/update`, `Order.place`); the canonical constructors accept any row already in the database.
- **Money** is `BigDecimal` (DECIMAL(10,2)) and serialised as a JSON number. Orders store `unitPrice` from the product at order time.
- **Timestamps**: `Instant` in the domain; columns are `TIMESTAMP(3)` *without* time zone holding UTC (as Prisma wrote them). Entities use `LocalDateTime` converted at UTC in `EntityMapper`; never set `hibernate.jdbc.time_zone` (it shifts values by the JVM zone). Tests run in UTC+14 to catch that.
- **Ids**: `IdGenerator` port → `CuidIdGenerator` (cuid v1 format like the existing rows).
- Use cases that write several rows run in `TransactionRunner.inTransaction` (Spring `TransactionTemplate`); adapters are `@Transactional` and join it.
- Never return `passwordHash` (`AdminUser.toString()` redacts it) or tokens other than the login response.
- Response shapes match `packages/types` (paginated lists `{ data, total, page, pageSize, totalPages }`).

## Auth

`adapter/out/password/Argon2PasswordHasher` verifies the PHC hashes created by node-argon2 (parameter order `m,p,t`, which Spring Security's `Argon2PasswordEncoder` rejects). JWT HS256 with the raw UTF-8 bytes of `JWT_SECRET` (≥ 32 bytes, else the app refuses to start), claims `{ sub, email, iat, exp }`, 8h: identical to the Node api's tokens, so they stay valid. Spring Security resource server in `security/SecurityConfiguration`: only `/api/admin/**` (except login) reads the `Authorization` header; 401 body `{"message":"Unauthorized"}`. The admin app stores the token in `localStorage` (`admin_token`).

## Turnstile (contact form)

`TurnstileCaptchaVerifier`: enforced only when `TURNSTILE_SECRET_KEY` is set (SealedSecret `fest-descans-api-turnstile`, optional in the chart; startup logs a WARN when it is not). Fails closed on errors/timeouts (5s). Visitor IP from `CF-Connecting-IP` (behind the tunnel the remote address is cloudflared). The token is never stored. New public write endpoints should reuse the `CaptchaVerifier` port.

## CORS

Origins from `WEB_URL` and `ADMIN_URL` (`festpage.web.cors-origins`, chart `cors.*`). A new frontend origin must be added there.

## Database and migrations

- **Flyway owns the schema**: `src/main/resources/db/migration/V<n>__<what>.sql`, applied by Boot's Flyway auto-configuration when the app starts (before Hibernate and before it reports ready; if PostgreSQL is down the app exits and Kubernetes restarts it). `V1__init.sql` is the schema the three Prisma migrations produced (merged into one script; verified identical).
- Production was created by Prisma, so `application.yml` has `spring.flyway.baseline-on-migrate: true` and `baseline-version: 1`: on that database Flyway only records a baseline at V1 (`PrismaBaselineTest`); an empty database runs V1. New changes start at `V2__...`. `_prisma_migrations` stays in the DB, unused.
- **Migrations must be backwards compatible** with the previous api version (rolling update): add columns nullable/with default; drop or rename in a later release. Never edit an applied migration.
- Identifiers are Prisma's quoted camelCase (`"Product"."createdAt"`): `PersistenceConfiguration` sets the standard physical naming strategy and globally quoted identifiers. Postgres enums `ProductCategory`/`OrderStatus` map with `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`; keep the Java enums, the SQL enums and `packages/types` in sync.
- Local data: `apps/api/dev/seed.sql` (admin `admin@festpage.com` / `admin123!` + 6 products).

## Tests (JUnit 5 / Jupiter, AssertJ)

`pnpm api:build` (= `cd apps/api && ./gradlew build`). All in `src/test`, by package:

| Package | What |
|---|---|
| `domain`, `application` | plain unit tests; services against the fakes in `application/fakes` (`InMemoryDatabase` implements every repository port, `RecordingCaptchaVerifier`, `SequentialIdGenerator`, `MutableClock`, `Fixtures`) |
| `adapter/in/web` | `@SpringBootTest` + `MockMvcTester` through the real services and the in-memory database, no DB (the old Fastify `inject` + fake Prisma tests): `PublicApiTest`, `AdminApiTest`, `SecurityTest` (incl. a token signed by the Node api) |
| `adapter/out/persistence` | real **PostgreSQL 18** (zonky embedded binaries, no Docker; `EmbeddedPostgresDatabase`, one fresh DB per class): JPA adapters |
| `adapter/out/password`, `adapter/out/turnstile` | Argon2 against a node-argon2 hash; Turnstile against a local `HttpServer` |
| root | `ApplicationStartupTest` (starts through `main()` on a real port, every use case wired, refuses to start without `JWT_SECRET`), `FestPageApplicationTest` (end to end with `dev/seed.sql`, health groups, endpoint exposure), `PrismaBaselineTest`, `HexagonalArchitectureTest` |

Test-only Spring setups (`WebTestBase.App`, `PersistenceAdaptersTest.App`) are deliberately plain classes (no `@Configuration`), so the app's component scan never picks them up.

Every new endpoint gets: happy path, validation 400, auth 401 (admin), and its error mapping (404/409). The `Image (api)` PR check also runs the image against the production Postgres image (migrations on startup, seed, login).
