# Snip – URL Shortener

A full-stack URL shortener built with **Java Spring Boot**, **React + TypeScript**, and **SQLite**, with Docker support for running the complete application.

## Features

* Create shortened URLs with optional custom aliases
* Automatically generate a unique alias when one is not provided
* Validate URLs and custom aliases
* Redirect shortened URLs to their original destinations
* Delete shortened URLs
* List URLs sorted by creation date (newest first)
* Paginate URL listings
* Persist data using SQLite
* Responsive React frontend
* Docker Compose setup for running the full application
* Backend unit and integration tests
* Frontend component and API tests

## Architecture

```text
frontend/
  React + TypeScript
  Vite
  Nginx

backend/
  Java 17
  Spring Boot
  JDBC
  SQLite

docker-compose.yml
  Orchestrates frontend and backend containers
```

In development, Vite proxies API requests to the local Spring Boot application.

In the production Docker setup, Nginx serves the compiled React application and proxies API and alias requests to the backend service. SQLite data is stored in a Docker volume so it persists when containers are recreated.

## Requirements

For local development:

* Java 17+
* Maven
* Node.js 20+
* npm

For the containerised setup:

* Docker
* Docker Compose

## Running with Docker

From the project root:

```bash
docker compose up --build
```

The frontend is then available at:

```text
http://localhost:3000
```

To stop the application:

```bash
docker compose down
```

The SQLite database is stored in a named Docker volume, so the data survives container restarts.

To remove the containers and persisted database volume:

```bash
docker compose down -v
```

## Running locally

### Backend

From `backend/`:

```bash
mvn spring-boot:run
```

The API runs on:

```text
http://localhost:8080
```

The database path can be configured using the `DATABASE_PATH` environment variable.

### Frontend

From `frontend/`:

```bash
npm install
npm run dev
```

The development frontend runs on:

```text
http://localhost:3000
```

Vite proxies `/shorten` and `/urls` requests to the backend during development.

## API Reference

| Method   | Path       | Description                                 |
| -------- | ---------- | ------------------------------------------- |
| `POST`   | `/shorten` | Create a shortened URL                      |
| `GET`    | `/urls`    | Retrieve a paginated list of shortened URLs |
| `GET`    | `/{alias}` | Redirect to the original URL                |
| `DELETE` | `/{alias}` | Delete a shortened URL                      |

### POST `/shorten`

Request:

```json
{
  "fullUrl": "https://example.com/very/long/path",
  "customAlias": "my-alias"
}
```

`customAlias` is optional.

Response:

```json
{
  "alias": "my-alias",
  "fullUrl": "https://example.com/very/long/path",
  "shortUrl": "http://localhost:8080/my-alias"
}
```

Returns `201 Created` on success.

Returns `400 Bad Request` if the URL or custom alias is invalid, or if the requested alias is already in use.

If no custom alias is supplied, the backend generates a unique random alias.

### GET `/urls`

The endpoint supports pagination using `page` and `size` query parameters.

Example:

```text
GET /urls?page=0&size=5
```

Response:

```json
{
  "urls": [
    {
      "alias": "my-alias",
      "fullUrl": "https://example.com/very/long/path",
      "shortUrl": "http://localhost:8080/my-alias"
    }
  ],
  "page": 0,
  "size": 5,
  "totalItems": 12,
  "totalPages": 3
}
```

URLs are returned in **descending creation-date order**, with the newest URLs first.

Pagination parameters have the following constraints:

* `page` must be `0` or greater
* `size` must be between `1` and `100`
* Default page: `0`
* Default page size: `5`

Invalid pagination parameters return `400 Bad Request` with a JSON error message.

### GET `/{alias}`

Returns:

* `302 Found` with a `Location` header containing the original URL when the alias exists
* `404 Not Found` when the alias does not exist

### DELETE `/{alias}`

Returns:

* `204 No Content` when the URL is successfully deleted
* `404 Not Found` when the alias does not exist

## Alias Handling

Custom aliases must:

* Be between 2 and 64 characters
* Contain only letters, numbers, and hyphens

For example:

```text
my-url       valid
example123   valid
my_url       invalid
a             invalid
my alias      invalid
```

When a custom alias is supplied, the service checks that it is not already in use.

When an alias is generated automatically, the `AliasGenerator` creates a random alias and checks it against the database. If a generated alias is already taken, generation is retried until a unique alias is found.

If a unique alias cannot be generated within the configured retry limit, the service returns a domain error rather than creating a duplicate URL.

## Error Handling

Application and validation errors are handled centrally using Spring's `@ControllerAdvice`.

Errors are returned in a consistent format:

```json
{
  "error": "Size must not exceed 100."
}
```

This covers both request validation errors and domain-level errors such as duplicate aliases or failure to generate a unique alias.

## Persistence

The application uses **SQLite** for persistence through Spring's `JdbcTemplate`.

The database location is configurable using:

```text
DATABASE_PATH
```

In Docker, the database is stored under `/data` and backed by a named Docker volume. This means the database survives container restarts and container recreation unless the volume is explicitly removed.

## Transactions

URL creation is performed within a service-layer transaction. This keeps the persistence operation within a clearly defined transactional boundary and allows database changes to be rolled back if an unexpected failure occurs during the operation.

## Testing

### Backend

The backend contains both unit and integration tests.

Run the backend tests with:

```bash
cd backend
mvn test
```

The integration tests exercise the HTTP endpoints and verify behaviour including:

* URL creation
* Alias validation
* Alias generation
* URL redirection
* URL deletion
* Pagination
* Creation-date sorting
* Pagination parameter validation
* Error responses

### Frontend

From `frontend/`:

```bash
npm test
```

A production frontend build can be created with:

```bash
npm run build
```

## Project Structure

```text
.
├── backend/
│   ├── src/
│   │   ├── main/
│   │   └── test/
│   ├── Dockerfile
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── hooks/
│   │   ├── services/
│   │   └── types/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
│
└── docker-compose.yml
```

## Design Decisions

### Backend pagination

Pagination is implemented in the backend rather than fetching all URLs and paginating in the browser. The database performs the `LIMIT` and `OFFSET` operations, which avoids transferring unnecessary records to the client as the dataset grows.

The API also returns `totalItems` and `totalPages`, allowing the frontend to provide appropriate pagination controls.

### Sorting

URLs are sorted by `created_at` in descending order at the database level, ensuring that pagination is applied consistently to the desired ordering.

### Frontend API handling

The React application keeps API and pagination state in the `useUrlShortener` hook. Components are responsible primarily for presentation, while the hook coordinates loading, errors, pagination, creation, and deletion.

### Reverse proxy

Nginx serves the production React build and proxies backend requests. This keeps the browser-facing application on a single origin and avoids requiring the frontend to know the backend container's internal address.

## Environment Configuration

The backend supports:

| Variable        | Description                   | Default                 |
| --------------- | ----------------------------- | ----------------------- |
| `DATABASE_PATH` | SQLite database file location | `/data/urlshortener.db` |
| `SERVER_PORT`   | Spring Boot HTTP port         | `8080`                  |

The frontend can optionally use:

```text
VITE_API_BASE_URL
```

When unset, the frontend uses relative API paths, allowing Nginx to handle the proxying in the Docker deployment.
