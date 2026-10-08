# DevShelf API

REST API for **DevShelf**, a bookstore for developers. It serves the book catalogue to the DevShelf
Next.js frontend: search, filter by category, sort, page, and create / edit / delete books.

Java 21 · Spring Boot 3.5 · Spring Data JPA · PostgreSQL · Flyway · springdoc-openapi · JUnit 5

| | |
|---|---|
| Live API | <https://devshelf-api.onrender.com/api/v1/books> (Render free plan. It sleeps after ~15 idle minutes and a cold start can take a few minutes, so [`keep-warm.yml`](.github/workflows/keep-warm.yml) pings it every 5 minutes) |
| Swagger UI | <https://devshelf-api.onrender.com/swagger-ui/index.html> |
| Frontend | <https://book-catalog-frontend-rouge.vercel.app/admin/books/list> · repo [book-catalog-frontend](https://github.com/Sachinsm7676/book-catalog-frontend) |

## Endpoints

All endpoints are under `/api/v1`.

| Method | Path | Success | Errors |
|---|---|---|---|
| GET | `/books?q=&category=&sort=&page=&size=` | 200 page of books | 400 invalid parameter |
| GET | `/books/{id}` | 200 book | 404 |
| POST | `/books` | 201 book + `Location: /api/v1/books/{id}` | 400, 409 |
| PUT | `/books/{id}` | 200 book | 400, 404, 409 |
| DELETE | `/books/{id}` | 204 | 404 |

**List parameters**

| Parameter | Default | Rules |
|---|---|---|
| `q` | none | Trimmed, max 100 chars. Case-insensitive "contains" on title, author or ISBN. `%` and `_` match literally. |
| `category` | all | A label: `JavaScript`, `Java`, `Python`, `DevOps`, `System Design`, `AI/ML`, `Databases`, or `All`. |
| `sort` | `popular` | `popular`, `newest`, `price-asc`, `price-desc`, `rating`, `title`, `updated` |
| `page` | 1 | 1-based. A page past the end returns the last page (and says so in `pageNumber`). |
| `size` | 8 | 1 to 50 |

```json
{"list":[ ...books... ],"pageNumber":1,"size":8,"totalElements":24,"totalPages":3}
```

**Request body** (POST and PUT): `title`, `author`, `category`, `priceInr`, `isbn`, `publishedAt`,
`description`, `coverUrl`. Strings are trimmed; optional fields that are blank become `null`; unknown
properties are ignored. `id`, `rating`, `ratingCount`, `popularity` and the timestamps are read-only.
The id is a slug of the title made once at creation (`-2`, `-3`, ... if taken) and never changes.

## Error shape

Every non-2xx response has the same body:

```json
{"status":400,"code":"VALIDATION_FAILED","message":"Please correct the highlighted fields.","fieldErrors":{"title":"Title is required."}}
```

| Status | `code` | When |
|---|---|---|
| 400 | `VALIDATION_FAILED` | A field or query parameter is invalid. One message per field, the first rule that fails. |
| 400 | `BAD_REQUEST` | The body is not readable JSON. |
| 404 | `BOOK_NOT_FOUND` / `NOT_FOUND` | Unknown book / unknown URL. |
| 405 | `METHOD_NOT_ALLOWED` | e.g. PATCH. |
| 409 | `ISBN_TAKEN` | Another book already has this ISBN (`fieldErrors.isbn`). |
| 415 | `UNSUPPORTED_MEDIA_TYPE` | Body is not `application/json`. |
| 500 | `INTERNAL_ERROR` | Unexpected; the stack trace is logged, never returned. |

The validation messages are a contract with the frontend, which shows them verbatim. They are all in
[`BookMessages.java`](src/main/java/com/devshelf/api/book/dto/BookMessages.java). A value of the wrong
JSON type (for example `"priceInr": "abc"`) is reported against its field, but JSON parsing stops at
that value, so it is the only field reported in that response.

## Example (captured from a running instance)

`GET /api/v1/books/clean-code-in-java`

```json
{"id":"clean-code-in-java","title":"Clean Code in Java","author":"Ananya Rao","category":"Java","priceInr":999.00,"isbn":"9789350000014","publishedAt":"2025-11-04","description":"Write Java that your teammates can read on the first pass: naming, small methods, honest tests and refactoring without fear, with examples drawn from real Spring services.","coverUrl":"/assets/images/book-cover-clean-code-in-java.jpg","rating":4.8,"ratingCount":1284,"popularity":100,"createdAt":"2026-10-07T18:34:45.545360Z","updatedAt":"2026-10-07T18:34:45.545360Z"}
```

`POST /api/v1/books` with an ISBN that is already used:

```json
{"status":409,"code":"ISBN_TAKEN","message":"Please correct the highlighted fields.","fieldErrors":{"isbn":"Another book already uses this ISBN."}}
```

## Run locally

Requires Java 21. Maven is not needed; use the bundled wrapper (`./mvnw`, or `mvnw.cmd` on Windows).
Flyway creates the table and seeds 24 books on first start.

**Option A: Docker Compose (PostgreSQL 16 on port 5432)**

```bash
docker compose up -d
./mvnw spring-boot:run
```

**Option B: an existing PostgreSQL**

```sql
CREATE ROLE devshelf LOGIN PASSWORD 'devshelf';
CREATE DATABASE devshelf OWNER devshelf;
```

```bash
DB_HOST=localhost DB_PORT=5432 DB_NAME=devshelf DB_USERNAME=devshelf DB_PASSWORD=devshelf ./mvnw spring-boot:run
```

PowerShell:

```powershell
$env:DB_PORT='5433'; $env:DB_NAME='devshelf_db'; .\mvnw.cmd spring-boot:run
```

The API listens on <http://localhost:8080>. Every setting is an environment variable; see
[`.env.example`](.env.example) for the full list (`PORT`, `DB_*`, `CORS_ALLOWED_ORIGINS`, `APP_TIMEZONE`).

| URL | |
|---|---|
| <http://localhost:8080/api/v1/books> | the API |
| <http://localhost:8080/swagger-ui.html> | Swagger UI |
| <http://localhost:8080/v3/api-docs> | OpenAPI JSON |
| <http://localhost:8080/actuator/health> | health check (the only actuator endpoint exposed) |

## Tests

```bash
./mvnw test
```

The tests need no database server: they run against H2 in PostgreSQL mode with the same Flyway
migrations, a fixed clock, and a rollback after each test. They cover listing, search, every sort,
paging and clamping, every validation message (including wrong JSON types), the ISBN conflict rules,
create / update / delete, CORS and the slug generator.

## Deploy to Render

1. Push this repository to GitHub.
2. In Render: **New > Blueprint**, pick the repository. [`render.yaml`](render.yaml) creates a free
   PostgreSQL database `devshelf-db` and a free Docker web service `devshelf-api` (region Singapore),
   passes the database's connection string as `DATABASE_URL`, and uses `/actuator/health` as the health check.
3. When prompted, set `CORS_ALLOWED_ORIGINS` to the deployed frontend origin, e.g.
   `https://devshelf.vercel.app` (comma-separate several; no trailing slash).

Free services sleep when idle, so the first request after a pause can take up to a minute.

> Render's Blueprint can pass a Postgres database only as `connectionString`
> (`postgresql://user:password@host[:port]/database`), not as separate host and port. When `DATABASE_URL`
> is set, `DatabaseUrlEnvironmentPostProcessor` converts it into the JDBC url, username and password and it
> wins over the `DB_*` variables; locally, leave it unset and use `DB_*`.

## Project layout

```
src/main/java/com/devshelf/api
├── book/            Book entity, repository, service, controller, validator, slug generator
│   └── dto/         request/response records, sort options, query parsing, messages
├── common/          error envelope and exception handling
└── config/          CORS, clock, request logging, OpenAPI, DATABASE_URL → JDBC settings
src/main/resources/db/migration   Flyway migrations (V1 schema, V2 seed)
```
