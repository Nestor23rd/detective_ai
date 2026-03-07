# AI Internet Detective

AI Internet Detective is a full-stack misinformation investigation platform built for hackathon use cases where suspicious content spreads through low-context formats such as WhatsApp forwards, screenshots, scanned notices, article links, and short video clips.

The project does not treat AI as a black-box answer generator. Instead, it uses ASI-1 to turn raw content into a structured investigation report with claims, evidence, source trails, reasoning, limitations, and recommended next checks.

## The problem

Most suspicious content does not arrive in a clean format. It arrives as:
- pasted text copied from social media
- screenshots of posts or messages
- article URLs with unknown credibility
- scanned PDF notices
- video links with weak or missing context

Most AI demos stop at a generic summary or an overconfident score. That is not enough for fact-checking, newsroom work, or public-trust scenarios.

This project is built around a different standard:
- show what the model found
- show what evidence supports or weakens each claim
- show the source trail
- show what remains uncertain
- avoid fake confidence when media cannot actually be inspected

## What the project does

The application accepts six kinds of investigation inputs:
- pasted text
- article URL
- image upload
- PDF upload
- video URL
- uploaded video file

For each submission, the system can return:
- summary
- extracted claims
- per-claim assessment
- per-claim confidence
- evidence bullets
- source URLs
- extracted entities
- language signal
- risk labels
- overall credibility score when appropriate
- verdict when appropriate
- explicit `Cannot analyze` status when the media is inaccessible or insufficient
- reasoning
- limitations
- recommended verification checks
- follow-up Q&A grounded in the saved report

## Why this project is different

The main value of AI Internet Detective is not that it produces a score. The value is that it produces a report someone can inspect.

Important product choices:
- Claim-level evidence instead of a single opaque answer
- Archived investigations instead of disposable one-off chats
- Follow-up questions on stored reports instead of re-running from scratch
- A clear distinction between suspicious content and content that simply cannot be inspected
- A workflow designed for multilingual and screenshot-heavy misinformation patterns

## How ASI-1 is used

ASI-1 is central to the product. The project would fail its purpose without it.

The backend uses ASI-1 in a structured multi-step flow:

1. Signal extraction pass
   The backend sends the normalized content to ASI-1 to extract:
   - summary
   - claims
   - entities
   - language
   - risk labels

2. Claim verification pass
   The backend sends the first-pass output into a second ASI-1 step that returns:
   - per-claim assessment
   - per-claim confidence
   - evidence bullets
   - source URLs
   - reasoning
   - limitations
   - recommended checks
   - final score and verdict when appropriate

3. Follow-up report Q&A
   After a report is saved, ASI-1 can be queried again with a follow-up question such as:
   - What is the weakest claim?
   - Which source matters most?
   - What should a human verify next?

Implementation details:
- ASI-1 is called only from the backend
- the API key never belongs in the frontend
- responses are requested as structured JSON
- web search is enabled for broader verification context
- image submissions use multimodal prompting
- video and inaccessible media are handled with explicit caution logic

## Product flow

1. The user opens the Angular frontend and chooses the appropriate analysis mode.
2. The frontend sends the content to the Spring Boot backend.
3. The backend normalizes the request and extracts readable text where needed.
4. If the input is a URL, the backend fetches and cleans article content.
5. If the input is a PDF, OCR-aware extraction can be used.
6. The backend runs the first ASI-1 pass for signal extraction.
7. The backend runs the second ASI-1 pass for claim verification and reasoning.
8. The result is normalized, scored, and stored in MongoDB.
9. The frontend renders the results page with sources, claims, and follow-up Q&A.
10. The archive page keeps recent reports searchable and reusable.

## Trust and guardrails

One of the most important behaviors in this project is honesty under uncertainty.

Example:
- if a video URL is broken, private, deleted, or does not expose enough retrievable data, the system does not force a fake low score
- instead, it returns `Cannot analyze`
- it explains why the content could not be inspected
- it recommends what a human should verify next

That behavior is important because many misinformation tools fail by pretending to know more than they actually do.

## Supported outputs in practice

The results page is designed to be inspectable by a human reviewer. A strong report can show:
- the main verdict or analysis status
- the confidence band or why confidence is missing
- risk labels such as misinformation, public safety, broken_link, or limited_analysis
- named people, organizations, places, or institutions
- claims with contradiction or support signals
- evidence points
- source URLs collected during investigation
- model reasoning
- limitations
- recommended checks

The archive page then stores those reports so the demo is not limited to a single run.

## Architecture

### Frontend
- Angular 19
- TypeScript
- TailwindCSS

Main frontend areas:
- `features/home`: landing page
- `features/analysis`: submission console
- `features/results`: evidence-led investigation report
- `features/history`: searchable archive
- `shared/components`: reusable UI pieces such as score visualization

### Backend
- Spring Boot 3
- Java 21
- REST API

Main backend areas:
- `analysis/controller`: HTTP endpoints
- `analysis/service`: orchestration and business logic
- `analysis/integration`: ASI-1 client
- `analysis/model`: MongoDB documents
- `analysis/dto`: request and response contracts
- `analysis/repository`: persistence

### Database
- MongoDB

MongoDB fits the project well because each investigation is naturally a document containing summary, claims, evidence points, sources, reasoning, and metadata.

## API summary

### `POST /api/analyze`
Analyze exactly one of:
- `text`
- `url`
- `imageBase64`
- `videoUrl`

Examples:

Text:
```json
{
  "text": "Forwarded as received: tomorrow all banks will freeze UPI for 48 hours"
}
```

Article URL:
```json
{
  "url": "https://example.com/news-story"
}
```

Image:
```json
{
  "imageBase64": "iVBORw0KGgoAAAANSUhEUgAA...",
  "imageMimeType": "image/png",
  "imageName": "viral-post.png"
}
```

Video URL:
```json
{
  "videoUrl": "https://example.com/clip.mp4"
}
```

### `POST /api/analyze/upload`
Multipart upload endpoint for:
- image
- PDF
- video file

Form fields:
- `file`
- `ocrEnabled`

### `GET /api/history`
Returns recent investigations for the archive page.

### `GET /api/history/{id}`
Returns one full saved report.

### `POST /api/history/{id}/follow-up`
Asks a grounded follow-up question on a saved report.

Example:
```json
{
  "question": "What is the weakest claim in this report?"
}
```

## Local setup

Create a `.env` file at the repository root:

```env
ASI_ONE_API_KEY=your_asi_1_key
ASI_ONE_MODEL=asi1
ASI_ONE_BASE_URL=https://api.asi1.ai/v1
ASI_ONE_WEB_SEARCH=true
MONGODB_URI=mongodb://localhost:27017/ai_detective
APP_ALLOWED_ORIGINS=http://localhost:4200
```

The backend is configured to load:
- `backend/.env` if present
- otherwise the root `.env`

## Run locally

### 1. Start MongoDB
```bash
docker compose up -d mongodb
```

### 2. Start the backend
```bash
cd backend
mvn spring-boot:run
```

Backend URL:
```text
http://localhost:8080
```

### 3. Start the frontend
```bash
cd frontend
npm install
npm start
```

Frontend URL:
```text
http://localhost:4200
```

## Demo path

The strongest demo path is:
1. open the home page
2. go to the analysis console
3. run a Hinglish or Hindi misinformation sample
4. show the live ASI-1 processing overlay
5. show claim-level evidence on the results page
6. ask one follow-up question
7. open the archive
8. show a saved video case with `Cannot analyze`

Detailed script:
- `docs/demo-script.md`

Architecture notes:
- `docs/architecture.md`

VPS deployment:
- `docs/vps-deployment.md`

## What to emphasize in a submission

If this project is shown to judges or reviewers, the strongest points are:
- ASI-1 is used as a structured investigation engine, not a generic chatbot
- the output is explainable and auditable
- the app handles text, links, screenshots, PDFs, and video-related inputs
- the product is multilingual-friendly
- the system is honest about limitations
- saved reports remain useful after the first run

## Current scope and future extensions

This project is intentionally focused on hackathon scope. It already covers the main investigation workflow, but future improvements could include:
- user-specific accounts and private histories
- stronger video frame analysis beyond metadata-driven caution
- deeper image forensics
- queueing for long-running investigations
- a mobile sharing flow for rumor-heavy messaging platforms

## Security note

Never commit a real `ASI_ONE_API_KEY`.

If a key was ever present in version control, rotate it immediately and treat it as compromised.
