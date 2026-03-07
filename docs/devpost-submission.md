# Devpost Submission Pack

## Project Name
AI Internet Detective

## Tagline
An ASI-1 powered misinformation investigation desk for text, screenshots, PDFs, article URLs, video links, and uploaded clips.

## Short Pitch
AI Internet Detective turns suspicious online content into evidence-led investigation reports with claim-level reasoning, source trails, and explicit uncertainty guardrails.

## One-Sentence Description
AI Internet Detective uses ASI-1 to investigate suspicious text, screenshots, documents, and video-related content, then returns structured reports with claims, evidence, sources, and verification guidance.

## Elevator Pitch
AI Internet Detective is built for the way misinformation actually spreads: low-context text, forwarded messages, screenshots, scanned notices, suspicious articles, and viral video links. Instead of returning a generic AI answer, it uses ASI-1 to generate an inspection-ready investigation report with extracted claims, claim-level evidence, source URLs, language signals, reasoning, and next verification steps. It also includes an important guardrail: if a video cannot actually be inspected, the system returns `Cannot analyze` instead of forcing a misleading fake score.

## Inspiration
We built AI Internet Detective because suspicious content rarely arrives in a clean or trustworthy format. It usually spreads as copied text, screenshots, low-context forwards, scanned PDFs, short clips, or social media links with missing provenance.

Most AI tools handle this badly in one of two ways:
- they produce a shallow summary with no evidence trail
- they generate overconfident verdicts even when the underlying media cannot actually be verified

That makes them weak for real fact-checking workflows, public-interest investigation, or trustworthy misinformation review. We wanted to build something closer to an investigation desk than a chatbot: something that shows claims, evidence, sources, limitations, and clear next steps.

## What it does
AI Internet Detective transforms messy online content into a structured investigation report that a human can actually inspect.

For every case, the system can surface:
- summary
- extracted claims
- claim-level assessments
- confidence scores
- evidence bullets
- source URLs
- named entities
- language and risk labels
- reasoning
- limitations
- recommended next checks

Most importantly, the product is designed to be honest about uncertainty. If a video URL is inaccessible or does not expose enough retrievable information, the system returns `Cannot analyze` rather than pretending to know more than it does.

## How we built it
We built AI Internet Detective as a full-stack application with:
- Angular 19, TypeScript, and TailwindCSS on the frontend
- Spring Boot 3 and Java 21 on the backend
- MongoDB for storing investigations
- ASI-1 as the core intelligence layer

The end-to-end workflow looks like this:

1. The user submits one of six input types: text, article URL, image, PDF, video URL, or uploaded video.
2. The backend normalizes the content and extracts readable text where needed.
3. ASI-1 runs a first pass to extract summary, claims, entities, language, and risk labels.
4. ASI-1 runs a second pass to verify claims, attach evidence points, return source URLs, and generate the final report.
5. The investigation is stored in MongoDB and displayed in the results page and archive.
6. The user can ask follow-up questions on the saved report using ASI-1 again.

ASI-1 is not a decorative integration. It is the core of the workflow.

We used ASI-1 in three meaningful ways:
- structured signal extraction
- claim verification and reasoning
- grounded follow-up Q&A on stored reports

Technical details:
- ASI-1 calls are made from the Spring Boot backend
- responses are requested as structured JSON
- web search is enabled for broader verification context
- image workflows use multimodal prompting
- video workflows include explicit guardrails for inaccessible media

## Additional ASI-1 details
ASI-1 is the core of the product, not an add-on.

We used ASI-1 in three meaningful ways:

1. Structured signal extraction
   ASI-1 extracts claims, entities, language, and risk labels from noisy input.

2. Claim verification and reasoning
   ASI-1 takes those extracted signals and returns claim-level assessments, confidence, evidence bullets, source URLs, limitations, and next verification steps.

3. Grounded follow-up Q&A
   After a report is saved, ASI-1 can answer focused questions such as:
   - What is the weakest claim?
   - Which source matters most?
   - What should a human verify next?

Technical details:
- ASI-1 calls are made from the Spring Boot backend
- responses are requested as structured JSON
- web search is enabled for broader verification context
- image workflows use multimodal prompting
- video workflows include explicit guardrails for inaccessible media

## Why This Project Stands Out
- It handles multiple real-world misinformation formats instead of a single clean prompt type.
- It gives claim-level evidence instead of only a summary or score.
- It keeps an archive of investigations instead of behaving like a disposable chatbot.
- It lets the user challenge the report with follow-up questions.
- It treats uncertainty honestly with `Cannot analyze` when media cannot be inspected.
- It is designed for multilingual and screenshot-heavy misinformation patterns common in Bharat and other fast-moving social ecosystems.

## Tech Stack
- Frontend: Angular 19, TypeScript, TailwindCSS
- Backend: Spring Boot 3, Java 21
- Database: MongoDB
- AI: ASI-1 API

## What We Built
- A landing page that clearly explains the product and supported modes
- A live analysis console with six input modes
- A processing UI that visualizes the ASI-1 investigation flow
- A results page with claim-level evidence and source trails
- A follow-up question panel for saved reports
- A searchable archive of previous investigations
- Guardrail logic for inaccessible video links and limited media analysis

## Challenges we ran into
- Designing structured output robustly enough for multiple media types
- Handling incomplete or malformed model output safely
- Avoiding misleading confidence when media was inaccessible
- Making the frontend demo feel polished and explainable under hackathon time pressure
- Keeping the product useful for both live demos and stored investigations

## Accomplishments that we're proud of
- We built a product that feels like an investigation tool rather than a chatbot wrapper.
- We added claim-level evidence and source URLs instead of stopping at a generic verdict.
- We built honest media guardrails so inaccessible videos are not mislabeled as fake.
- We created a demo-ready UI that shows the AI workflow clearly while keeping the final report inspectable.
- We made the report reusable by storing investigations and enabling follow-up questions.

## What we learned
- Trustworthy AI products need explicit uncertainty handling, not just better prompts.
- Structured multi-step AI workflows are much more useful than single-pass completions.
- A strong demo is not only about accuracy; it is also about clarity, inspectability, and user trust.
- Misinformation tools need to work with messy real-world formats, not only ideal text inputs.

## What's next for AI Internet Detective
- Add stronger video frame analysis beyond metadata-driven caution
- Add direct image forensics and reverse-search support
- Add user accounts and private case histories
- Add mobile-friendly rumor sharing flows
- Add deployment and team-based review workflows for moderation or newsroom use cases

## Demo Walkthrough
Recommended order for the video:
1. Open the home page
2. Run a Hinglish or Hindi sample from the analysis page
3. Show the ASI-1 live processing overlay
4. Show the claim-level results report
5. Ask a follow-up question
6. Open the archive
7. Show a saved video report with `Cannot analyze`

## Submission Closing Paragraph
AI Internet Detective is our attempt to build a more trustworthy AI investigation workflow for the way misinformation actually spreads online. By combining ASI-1 with structured claim extraction, verification, source trails, follow-up Q&A, and explicit uncertainty guardrails, we built a tool that is not only useful in a demo, but also defensible under scrutiny.
