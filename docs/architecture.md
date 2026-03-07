# AI Internet Detective Architecture

## Stack
- Frontend: Angular 19 + TypeScript + TailwindCSS
- Backend: Spring Boot + Java 21 + REST API
- Database: MongoDB
- AI: ASI-1 Chat Completions API with strict JSON schema output and a two-pass verification workflow

## Request flow
1. User submits text or a URL from the Angular app.
2. `POST /api/analyze` reaches Spring Boot.
3. If the request contains a URL, the backend fetches the article and extracts readable paragraphs with Jsoup.
4. The backend sends a first-pass extraction prompt to ASI-1 for summary, claims, entities, language, and risk labels.
5. The backend sends a second-pass verification prompt to ASI-1 for per-claim evidence, confidence, source URLs, limitations, and final verdict.
6. The analysis result is normalized, scored, and stored in MongoDB.
7. Angular renders the credibility dashboard and can later reload the same report from `/api/history/{id}`.

## Backend package layout
- `config`: properties, CORS, and outbound REST client configuration.
- `analysis/controller`: REST endpoints and global exception handling.
- `analysis/service`: orchestration, prompt generation, and URL extraction.
- `analysis/integration`: ASI-1 client.
- `analysis/model`: MongoDB document model.
- `analysis/dto`: request and response contracts.
- `analysis/repository`: Spring Data Mongo repository.

## Frontend feature layout
- `core/models`: shared TypeScript contracts.
- `core/services`: API calls.
- `core/store`: lightweight signal-based state.
- `features/home`: landing page.
- `features/analysis`: submission form.
- `features/results`: credibility dashboard.
- `features/history`: archived investigations.
- `shared/components`: reusable navigation and score visualization.

## Why MongoDB here
MongoDB fits the hackathon scope well because each investigation naturally behaves like a document: summary, claims, evidence points, source URLs, entities, verdict, and reasoning all belong together.

## Next improvements
- Add authentication and user-specific history.
- Add direct screenshot/video frame forensics beyond OCR and metadata.
- Add queueing for long-running URL investigations.
- Add a dedicated mobile sharing flow for WhatsApp and Telegram rumors.
