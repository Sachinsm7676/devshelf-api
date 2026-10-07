# CLAUDE.md

Guide for AI coding agents working in this repository. Read it before changing anything.

## What this is

DevShelf API: a Spring Boot REST API serving a book catalogue to a separate Next.js frontend
(browser calls, so CORS matters). One entity, `Book`, under `/api/v1/books`. See `README.md` for the
endpoint and error contract.

## Stack

Java 21, Spring Boot 3.5 (web, data-jpa, validation, actuator), PostgreSQL, Flyway,
springdoc-openapi 2.8, JUnit 5 + MockMvc with H2 (PostgreSQL mode) for tests. Deployed to Render as a
Docker image (`Dockerfile`, `render.yaml`).

## Commands

| | |
|---|---|
| Run tests | `./mvnw test` (Windows: `.\mvnw.cmd test`) - no database server needed |
| Run locally | `docker compose up -d` then `./mvnw spring-boot:run` |
| Run against another Postgres | set `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` |
| Build jar | `./mvnw -DskipTests package` |
| Swagger UI | <http://localhost:8080/swagger-ui.html> |

## Folders

- `src/main/java/com/devshelf/api/book` - entity, repository, service, controller, request validator, slug generator
- `src/main/java/com/devshelf/api/book/dto` - request/response records, `BookSort`, `BookSearchQuery`, `BookMessages`
- `src/main/java/com/devshelf/api/common` - `ApiError` envelope, exceptions, `GlobalExceptionHandler`
- `src/main/java/com/devshelf/api/config` - CORS, `Clock`, request log filter, OpenAPI
- `src/main/resources/db/migration` - Flyway migrations
- `src/test/java/...` - MockMvc API tests (`ApiTestSupport` is the shared base) and unit tests

## Rules

1. **Messages are a contract with the frontend.** Every validation message lives in `BookMessages` and
   the frontend shows it verbatim and mirrors the rule. Never reword one, and never return two messages
   for one field (the first failing rule wins). Changing a message means changing the frontend too.
2. **Ids are slugs and immutable.** The id is generated from the title on create and never changes on
   update, even if the title does. Frontend URLs depend on it.
3. **Flyway migrations are append-only.** Never edit an applied migration (`V1__`, `V2__`, ...);
   add `V3__...` instead. SQL must run on both PostgreSQL and H2 in PostgreSQL mode (in H2, `DEFAULT`
   must come before `NOT NULL`).
4. **Never commit `.env`** or any real credentials. Configuration comes from environment variables;
   `.env.example` documents them and must list every variable the app reads.
5. **Keep filtering, sorting and paging in the database** (`BookRepository` JPQL). No in-memory filtering.
6. **Error responses always use `ApiError`** (`status`, `code`, `message`, `fieldErrors`). Never return a
   stack trace or exception text to the client.
7. **"Today" comes from the injected `Clock`** (zone `app.timezone`, Asia/Kolkata). Never call
   `LocalDate.now()` without it; tests rely on a fixed clock.
8. Add or update a test for every behaviour change, and run `./mvnw test` before calling work done.

## Gotchas

- Spring Data ignores `Sort.Order.nullsLast()` on `@Query` methods. Books without a published date
  sort last because of `hibernate.order_by.default_null_ordering=last` in `application.properties`.
- Hibernate only emits `NULLS LAST` when the dialect's default ordering differs, so do not change H2's
  `DEFAULT_NULL_ORDERING` in the test URL - it would make Hibernate's assumption about H2 wrong.
- A wrong JSON type (e.g. `"priceInr": "abc"`) is handled in `BookController#unreadableBody` from the
  Jackson path; parsing stops there, so only that field is reported.
