# 🔍 JVM Search Engine

A full-stack, from-scratch search engine built on the JVM. It crawls the web, parses and indexes pages using a custom inverted index with TF-IDF ranking, and exposes a clean REST API consumed by a modern React + Vite frontend.

---

## 📌 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Features](#features)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Backend Setup](#backend-setup)
  - [Frontend Setup](#frontend-setup)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

JVM Search Engine is a self-hosted, full-stack search engine that demonstrates core information retrieval concepts implemented from the ground up:

- A **multi-threaded web crawler** (Jsoup-powered) with `robots.txt` compliance and a persistent URL frontier
- An HTML **parser** that extracts text content, metadata, and outbound links
- An NLP **processing pipeline**: tokenization → normalization → stop-word removal → stemming → frequency analysis
- A persistent **inverted index** with TF-IDF scoring stored in PostgreSQL
- A **search service** with query parsing, ranked retrieval, and snippet generation
- An **analytics layer** tracking search history and aggregate statistics
- A **React + Vite frontend** with a Google-style UX — including a search bar, results page, crawl manager, and analytics dashboard

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                     Frontend (React + Vite)                   │
│     Home · SearchResults · CrawlerAdmin · AnalyticsAdmin      │
└────────────────────────────┬─────────────────────────────────┘
                             │  REST (HTTP/JSON)
┌────────────────────────────▼─────────────────────────────────┐
│                    Spring Boot Backend                        │
│                                                               │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────────┐  │
│  │ Crawler  │→ │  Parser  │→ │Processing│→ │  Indexing   │  │
│  │(Jsoup +  │  │(HTML,    │  │(Tokenize,│  │(InvertedIdx,│  │
│  │Frontier) │  │ Robots)  │  │ Stem…)   │  │ TF-IDF)     │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────┬──────┘  │
│                                                    │          │
│  ┌──────────┐  ┌──────────┐  ┌────────────────┐   │          │
│  │Analytics │  │Scheduler │  │  Search Service│◄──┘          │
│  │(History, │  │(Cron     │  │(Ranking,Snippet│              │
│  │ Stats)   │  │ Jobs)    │  │  Query Parser) │              │
│  └──────────┘  └──────────┘  └────────────────┘              │
└────────────────────────────┬─────────────────────────────────┘
                             │  JPA / HikariCP
                    ┌────────▼────────┐
                    │   PostgreSQL    │
                    └─────────────────┘
```

---

## Tech Stack

### Backend

| Layer       | Technology                  |
|-------------|-----------------------------|
| Language    | Java 21                     |
| Framework   | Spring Boot 4.1             |
| ORM         | Spring Data JPA / Hibernate |
| Database    | PostgreSQL                  |
| Web Crawler | Jsoup 1.17.2                |
| Boilerplate | Lombok                      |
| API Docs    | OpenAPI / Swagger           |
| Build       | Maven (via `mvnw` wrapper)  |
| Pool        | HikariCP                    |

### Frontend

| Layer     | Technology                       |
|-----------|----------------------------------|
| Language  | JavaScript (JSX)                 |
| Framework | React 19 + Vite 8                |
| Styling   | Tailwind CSS 4                   |
| Routing   | React Router v7                  |
| HTTP      | Axios (centralised service layer)|
| Charts    | Recharts                         |
| Icons     | Lucide React                     |

---

## Project Structure

```
jvm-search-engine/
│
├── backend/                            # Spring Boot application
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/jvmservicengine/search/
│       │   │   ├── SearchApplication.java
│       │   │   │
│       │   │   ├── api/                # REST controllers + DTOs
│       │   │   │   ├── controller/
│       │   │   │   │   ├── SearchController.java      # GET /api/v1/search
│       │   │   │   │   └── CrawlController.java       # POST /api/crawl
│       │   │   │   └── dto/
│       │   │   │       ├── request/
│       │   │   │       └── response/
│       │   │   │
│       │   │   ├── config/             # AsyncConfig, WebConfig, OpenApiConfig, DatabaseConfig
│       │   │   ├── common/             # Constants, enums, exceptions, utils, validation
│       │   │   │
│       │   │   ├── crawler/            # Crawler engine
│       │   │   │   ├── jsoup/          # Jsoup-based page fetcher
│       │   │   │   ├── robots/         # robots.txt parser and cache
│       │   │   │   └── service/        # CrawlerService (seed + start)
│       │   │   │
│       │   │   ├── parser/             # HTML to structured data
│       │   │   ├── processing/         # NLP pipeline
│       │   │   │   ├── tokenizer/
│       │   │   │   ├── normalization/
│       │   │   │   ├── stopwords/
│       │   │   │   ├── stemming/
│       │   │   │   └── frequency/
│       │   │   │
│       │   │   ├── indexing/           # Inverted index + TF-IDF
│       │   │   │   ├── invertedindex/
│       │   │   │   ├── postings/
│       │   │   │   ├── dictionary/
│       │   │   │   ├── tfidf/
│       │   │   │   └── service/        # IndexingService (flush to DB)
│       │   │   │
│       │   │   ├── searches/           # Search orchestration
│       │   │   │   ├── service/        # SearchService
│       │   │   │   ├── query/          # Query parser
│       │   │   │   ├── snippet/        # Snippet extractor
│       │   │   │   └── dto/
│       │   │   │
│       │   │   ├── ranking/            # RankingService (TF-IDF scoring)
│       │   │   │
│       │   │   ├── analytics/
│       │   │   │   ├── searchhistory/  # Tracks every query
│       │   │   │   └── statistics/    # SiteStatsService, StatisticsController
│       │   │   │
│       │   │   ├── scheduler/          # CrawlScheduler, ReindexScheduler
│       │   │   └── storage/           # JPA entities, repositories, mappers
│       │   │
│       │   └── resources/
│       │       └── application.properties
│       │
│       └── test/
│
└── frontend/                          # React + Vite application
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    ├── .env                           # VITE_API_BASE_URL
    ├── public/
    │   ├── favicon.ico
    │   └── logo.png
    └── src/
        ├── components/                # Reusable UI components
        │   ├── SearchBox.jsx
        │   ├── SearchResultItem.jsx
        │   ├── SearchResultSkeleton.jsx
        │   ├── SearchEmpty.jsx
        │   ├── SearchError.jsx
        │   ├── SearchInfo.jsx
        │   └── Pagination.jsx
        ├── pages/
        │   ├── Home.jsx
        │   ├── SearchResults.jsx
        │   ├── CrawlerAdmin.jsx       # Trigger and monitor crawl jobs
        │   └── AnalyticsAdmin.jsx     # Stats dashboard
        ├── layouts/                   # MainLayout.jsx
        ├── hooks/                     # useSearch.js, useCrawler.js
        ├── services/                  # Axios-based API layer
        │   ├── api.js                 # Axios instance + interceptors
        │   ├── search.service.js
        │   ├── crawler.service.js
        │   └── analytics.service.js
        ├── context/                   # SearchContext.jsx
        ├── store/                     # searchStore.js
        ├── routes/                    # AppRoutes.jsx
        ├── styles/                    # globals.css
        ├── App.jsx
        └── main.jsx
```

---

## Features

### ✅ Backend

- **Web Crawler** — Multi-threaded Jsoup crawler; respects `robots.txt`; supports seed-URL injection and async crawl start via `CrawlerService`
- **HTML Parser** — Extracts page title, body text, meta tags, and outbound hyperlinks
- **NLP Processing Pipeline** — Tokenization → lowercasing/normalization → stop-word filtering → stemming → term-frequency counting
- **Inverted Index** — PostgreSQL-backed inverted index with postings lists and a term dictionary; supports manual flush via `/api/crawl/flush-index`
- **TF-IDF Ranking** — Dedicated `RankingService` for ranked result retrieval
- **Search Service** — Full-text query parsing and ranked result retrieval with paginated `SearchResponse`
- **Snippet Generation** — Context-aware snippet extraction from indexed documents
- **Analytics** — Search history tracking and aggregate site statistics (`SiteStatsService`)
- **Crawl Queue Monitoring** — Real-time counts of PENDING / PROCESSING / DONE / FAILED items + error listing (top 50)
- **Scheduled Jobs** — `CrawlScheduler` and `ReindexScheduler` for background maintenance
- **REST API** — `SearchController`, `CrawlController`, `StatisticsController`; CORS open and documented with OpenAPI/Swagger
- **Async Processing** — Configurable thread-pool via `AsyncConfig`
- **HikariCP** — Production-grade connection pooling (max 10 connections, min 5 idle)

### ✅ Frontend

- **Home Page** — Google-style search landing page
- **Search Results Page** — Ranked results with snippets, pagination, loading skeletons, and empty/error states
- **Crawler Admin** — Submit a seed URL, trigger crawl jobs, flush the index, and monitor the crawl queue in real time
- **Analytics Dashboard** — Search history (paginated), search metrics, and aggregate engine statistics rendered with Recharts
- **Centralised API Layer** — Single Axios instance with request/response interceptors and structured error objects
- **Service Layer** — `searchService`, `crawlerService`, `analyticsService`
- **Custom Hooks** — `useSearch`, `useCrawler`
- **React Context + Store** — Shared search state management

---

## Getting Started

### Prerequisites

| Requirement | Version  |
|-------------|----------|
| Java        | 21+      |
| Maven       | 3.9+ (or use the included `mvnw` wrapper) |
| Node.js     | 18+      |
| npm         | Latest   |
| PostgreSQL  | 14+      |

---

### Backend Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Ananthchavan/jvm-search-engine.git
   cd jvm-search-engine
   ```

2. **Create the PostgreSQL database**
   ```sql
   CREATE DATABASE jvm_search_db;
   ```

3. **Configure environment variables** (or edit `application.properties` directly):
   ```bash
   # Linux / macOS
   export DB_HOST=localhost
   export DB_PORT=5432
   export DB_NAME=jvm_search_db
   export DB_USER=postgres
   export DB_PASS=postgres

   # Windows PowerShell
   $env:DB_HOST="localhost"; $env:DB_PORT="5432"; $env:DB_NAME="jvm_search_db"; $env:DB_USER="postgres"; $env:DB_PASS="postgres"
   ```

4. **Build and run**
   ```bash
   cd backend
   ./mvnw spring-boot:run        # Linux / macOS
   .\mvnw.cmd spring-boot:run    # Windows
   ```
   The API will be available at `http://localhost:8080`.

5. **Swagger UI** — `http://localhost:8080/swagger-ui.html`

---

### Frontend Setup

1. **Install dependencies**
   ```bash
   cd frontend
   npm install
   ```

2. **(Optional) Configure the API base URL**

   The frontend reads `VITE_API_BASE_URL` from `frontend/.env`. The default is already set to the local backend:
   ```env
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   ```

3. **Start the dev server**
   ```bash
   npm run dev
   ```
   The app will be available at `http://localhost:5173`.

> **Note:** Start the backend before starting the frontend.

---

## Configuration

### `backend/src/main/resources/application.properties`

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/jvm_search_db` | JDBC URL |
| `spring.datasource.username` | `postgres` | DB username |
| `spring.datasource.password` | `postgres` | DB password |
| `spring.jpa.hibernate.ddl-auto` | `create` | Schema strategy (`create`, `update`, `validate`) |
| `spring.jpa.show-sql` | `true` | Log generated SQL |
| `spring.datasource.hikari.maximum-pool-size` | `10` | Max DB connections |
| `spring.datasource.hikari.minimum-idle` | `5` | Min idle connections |
| `spring.datasource.hikari.idle-timeout` | `300000` | Idle timeout (ms) |

Environment variable overrides: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`.

### `frontend/.env`

| Variable | Default | Description |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api/v1` | Base URL for all API calls |

---

## API Reference

> Full interactive docs are at `http://localhost:8080/swagger-ui.html` when the backend is running.

### Search

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/v1/search?q={query}&page={n}` | Execute a paginated search query |

### Crawler

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/crawl` | Start a crawl job — body: `{ "seedUrl": "https://..." }` |
| `POST` | `/api/crawl/flush-index` | Manually flush in-memory index to PostgreSQL |
| `GET`  | `/api/crawl/stats` | Queue stats: pending / processing / completed / failed counts |
| `GET`  | `/api/crawl/errors` | Last 50 failed crawl queue items |

### Analytics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/v1/analytics/stats` | Aggregate site/engine statistics |
| `GET`  | `/api/v1/analytics/history?page={n}&size={s}` | Paginated search history |
| `GET`  | `/api/v1/analytics/history/metrics` | Aggregate search metrics |

### Statistics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET`  | `/api/v1/statistics` | Retrieve site statistics (via `StatisticsController`) |

---

## Roadmap

- [ ] Docker + Docker Compose support
- [ ] Security layer (JWT / OAuth2)
- [ ] PageRank integration for improved ranking
- [ ] Distributed crawling with multiple worker nodes
- [ ] Elasticsearch as an optional index backend
- [ ] Autocomplete and spell-check endpoints
- [ ] Dark mode toggle in the frontend
- [ ] Align `CrawlController` base path to `/api/v1/` convention

---

## Contributing

Contributions are welcome! Please open an issue to discuss your idea before submitting a pull request.

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/my-feature`
3. Commit your changes: `git commit -m "feat: add my feature"`
4. Push to your branch: `git push origin feature/my-feature`
5. Open a Pull Request

---

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
