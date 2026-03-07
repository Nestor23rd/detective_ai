import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-home-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section class="px-4 pb-24 pt-6 md:px-8 xl:px-10 2xl:px-12">
      <div class="mx-auto max-w-[1680px] space-y-8">
        <section class="shell-card relative overflow-hidden px-6 py-8 md:px-10 md:py-10 xl:px-12 xl:py-12">
          <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_14%_14%,rgba(224,90,71,0.18),transparent_18%),radial-gradient(circle_at_82%_18%,rgba(41,82,69,0.14),transparent_18%),radial-gradient(circle_at_66%_76%,rgba(56,189,248,0.12),transparent_22%)]"></div>
          <div class="pointer-events-none absolute inset-0 ghost-grid opacity-60"></div>

          <div class="relative grid gap-8 xl:grid-cols-[1.06fr_0.94fr] xl:items-start">
            <div>
              <span class="eyebrow">ASI-1 Powered Investigation Desk</span>
              <h1 class="mt-6 max-w-5xl text-5xl font-semibold leading-tight text-ink md:text-7xl">
                Build trust faster with claim-level evidence, source trails, and video-aware fact checking.
              </h1>
              <p class="mt-6 max-w-3xl text-lg leading-8 text-ink-soft">
                AI Internet Detective turns suspicious claims, article URLs, screenshots, PDFs, video links, and uploaded clips into structured investigations. Every report shows what was checked, what remains uncertain, and when media should be marked as cannot analyze instead of fake.
              </p>

              <div class="mt-8 flex flex-col gap-4 sm:flex-row">
                <a routerLink="/analysis" class="inline-flex items-center justify-center rounded-full bg-ink px-6 py-3 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:bg-ink-soft">
                  Run Live Investigation
                </a>
                <a routerLink="/history" class="inline-flex items-center justify-center rounded-full border border-ink/10 bg-white/80 px-6 py-3 text-sm font-semibold text-ink transition hover:border-ink/20 hover:bg-white">
                  Open Case Archive
                </a>
              </div>

              <div class="mt-8 grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <article *ngFor="let mode of inputModes" class="rounded-[22px] border border-slate-200/80 bg-white/72 p-4 shadow-[0_12px_30px_rgba(15,27,45,0.05)] backdrop-blur transition hover:-translate-y-0.5 hover:border-ink/15 hover:bg-white">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">{{ mode.kicker }}</p>
                  <p class="mt-2 text-base font-semibold text-ink">{{ mode.title }}</p>
                  <p class="mt-2 text-sm leading-6 text-ink-soft">{{ mode.detail }}</p>
                </article>
              </div>

              <div class="mt-8 grid gap-4 sm:grid-cols-3">
                <div *ngFor="let stat of heroStats" class="hero-stat">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">{{ stat.label }}</p>
                  <p class="mt-3 text-3xl font-semibold text-ink">{{ stat.value }}</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">{{ stat.detail }}</p>
                </div>
              </div>
            </div>

            <div class="grid gap-4">
              <div class="rounded-[32px] bg-[#08152c] p-6 text-white shadow-dossier">
                <div class="flex items-start justify-between gap-4">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.3em] text-sky-200/80">Live Report Snapshot</p>
                    <h2 class="mt-3 text-2xl font-semibold">Viral subsidy reel investigation</h2>
                    <p class="mt-2 text-sm leading-7 text-sky-100/75">Static preview of the kind of report the jury sees after one run through the ASI-1 workflow.</p>
                  </div>
                  <span class="rounded-full border border-sky-300/20 bg-sky-300/10 px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.24em] text-sky-100">
                    Structured output
                  </span>
                </div>

                <div class="mt-5 grid gap-3 sm:grid-cols-2">
                  <article *ngFor="let metric of previewMetrics" class="rounded-[22px] border border-white/10 bg-white/5 px-4 py-4">
                    <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-sky-100/60">{{ metric.label }}</p>
                    <p class="mt-2 text-lg font-semibold text-white">{{ metric.value }}</p>
                    <p class="mt-1 text-xs leading-6 text-slate-300">{{ metric.detail }}</p>
                  </article>
                </div>

                <div class="mt-5 rounded-[24px] border border-white/10 bg-white/5 p-4">
                  <div class="flex items-center justify-between gap-3">
                    <p class="text-xs font-semibold uppercase tracking-[0.28em] text-sky-100/70">Claim Board</p>
                    <span class="rounded-full bg-white/8 px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.22em] text-sky-100/80">Evidence-led</span>
                  </div>

                  <div class="mt-4 space-y-3">
                    <article *ngFor="let claim of previewClaims" class="rounded-[20px] border border-white/10 bg-[#10203d] px-4 py-4">
                      <div class="flex flex-wrap items-center gap-2">
                        <span class="rounded-full border px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.22em]" [ngClass]="claim.tone">
                          {{ claim.status }}
                        </span>
                        <span class="text-[11px] uppercase tracking-[0.22em] text-slate-400">{{ claim.confidence }}</span>
                      </div>
                      <p class="mt-3 text-sm font-semibold text-white">{{ claim.title }}</p>
                      <p class="mt-2 text-sm leading-6 text-slate-300">{{ claim.detail }}</p>
                    </article>
                  </div>
                </div>

                <div class="mt-5 rounded-[22px] border border-amber-300/15 bg-amber-300/10 px-4 py-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-amber-200">Media Guardrail</p>
                  <p class="mt-2 text-sm leading-7 text-amber-50">Broken or private video links return <span class="font-semibold">Cannot analyze</span>, not a misleading fake score. The product explains the limitation and recommends the next check.</p>
                </div>
              </div>

              <div class="grid gap-4 sm:grid-cols-2">
                <article class="soft-panel p-5">
                  <p class="text-xs font-semibold uppercase tracking-[0.28em] text-ember">Follow-up Q&A</p>
                  <p class="mt-3 text-sm leading-7 text-ink-soft">Ask the finished report which claim is weakest, which URL matters most, or what a human should verify next.</p>
                </article>
                <article class="soft-panel p-5">
                  <p class="text-xs font-semibold uppercase tracking-[0.28em] text-pine">Shareable output</p>
                  <p class="mt-3 text-sm leading-7 text-ink-soft">The results page packages score, status, claim evidence, sources, and reasoning in a demo-ready format.</p>
                </article>
              </div>
            </div>
          </div>
        </section>

        <section class="grid gap-6 xl:grid-cols-[0.92fr_1.08fr]">
          <article class="shell-card p-6 md:p-7">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.3em] text-steel">Built For Bharat</p>
                <h2 class="mt-3 text-3xl font-semibold text-ink">Designed around real misinformation flows, not generic prompts.</h2>
              </div>
              <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">Use cases</span>
            </div>

            <div class="mt-6 grid gap-4">
              <article *ngFor="let useCase of bharatUseCases" class="rounded-[24px] border border-slate-200 bg-slate-50/85 p-5 transition hover:-translate-y-0.5 hover:border-ink/15 hover:bg-white">
                <div class="flex flex-wrap items-center gap-3">
                  <span class="rounded-full bg-white px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-steel">{{ useCase.tag }}</span>
                  <span class="text-[11px] font-semibold uppercase tracking-[0.2em] text-ember">{{ useCase.input }}</span>
                </div>
                <p class="mt-3 text-lg font-semibold text-ink">{{ useCase.title }}</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">{{ useCase.detail }}</p>
              </article>
            </div>
          </article>

          <article class="shell-card p-6 md:p-7">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.3em] text-steel">Why This Demos Strongly</p>
                <h2 class="mt-3 text-3xl font-semibold text-ink">The product proves depth of ASI-1 usage, not just one prompt and a score.</h2>
              </div>
              <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">ASI-1 depth</span>
            </div>

            <div class="mt-6 grid gap-4 md:grid-cols-2">
              <article *ngFor="let card of proofCards" class="rounded-[24px] border border-slate-200 bg-white p-5 shadow-[0_12px_30px_rgba(15,27,45,0.04)]">
                <p class="text-[11px] font-semibold uppercase tracking-[0.24em]" [ngClass]="card.tone">{{ card.kicker }}</p>
                <p class="mt-3 text-lg font-semibold text-ink">{{ card.title }}</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">{{ card.detail }}</p>
              </article>
            </div>
          </article>
        </section>

        <section class="grid gap-6 lg:grid-cols-[1.04fr_0.96fr]">
          <article class="shell-card p-6 md:p-7">
            <div class="flex items-center justify-between gap-4">
              <div>
                <p class="text-xs font-semibold uppercase tracking-[0.3em] text-steel">End-To-End Flow</p>
                <h2 class="mt-3 text-3xl font-semibold text-ink">From raw signal to explainable verdict in one guided sequence.</h2>
              </div>
              <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">3 steps</span>
            </div>

            <div class="mt-6 grid gap-4 lg:grid-cols-3">
              <article *ngFor="let step of workflowSteps" class="rounded-[24px] border border-slate-200 bg-slate-50/85 p-5">
                <p class="text-xs font-semibold uppercase tracking-[0.28em] text-steel">{{ step.step }}</p>
                <p class="mt-3 text-xl font-semibold text-ink">{{ step.title }}</p>
                <p class="mt-3 text-sm leading-7 text-ink-soft">{{ step.detail }}</p>
              </article>
            </div>
          </article>

          <article class="shell-card overflow-hidden p-0">
            <div class="bg-[linear-gradient(135deg,#0f1b2d_0%,#17304f_55%,#295245_100%)] px-6 py-7 text-white md:px-7">
              <p class="text-xs font-semibold uppercase tracking-[0.3em] text-white/60">What The Final Report Exposes</p>
              <h2 class="mt-3 text-3xl font-semibold">Everything a judge needs to trust the output fast.</h2>
              <p class="mt-3 max-w-xl text-sm leading-7 text-white/78">The report is built to survive scrutiny: visible sources, claim confidence, limitations, recommended checks, and an explicit status when the media itself cannot be inspected.</p>
            </div>

            <div class="grid gap-4 p-6 md:p-7">
              <article *ngFor="let item of evidenceHighlights" class="rounded-[22px] border border-slate-200 bg-slate-50/85 px-4 py-4">
                <div class="flex items-center justify-between gap-3">
                  <p class="text-sm font-semibold text-ink">{{ item.title }}</p>
                  <span class="rounded-full bg-white px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.22em] text-steel">{{ item.badge }}</span>
                </div>
                <p class="mt-2 text-sm leading-7 text-ink-soft">{{ item.detail }}</p>
              </article>

              <div class="rounded-[24px] border border-ink/10 bg-white px-5 py-5">
                <p class="text-xs font-semibold uppercase tracking-[0.28em] text-steel">Ready To Demo</p>
                <p class="mt-3 text-base font-semibold text-ink">Open the analysis console, run a live case, then jump straight into the results and archive.</p>
                <div class="mt-4 flex flex-col gap-3 sm:flex-row">
                  <a routerLink="/analysis" class="inline-flex items-center justify-center rounded-full bg-ember px-5 py-3 text-sm font-semibold text-white transition hover:bg-[#cc4d3a]">
                    Launch Investigation
                  </a>
                  <a routerLink="/history" class="inline-flex items-center justify-center rounded-full border border-ink/10 bg-slate-50 px-5 py-3 text-sm font-semibold text-ink transition hover:border-ink/20 hover:bg-white">
                    Show Stored Reports
                  </a>
                </div>
              </div>
            </div>
          </article>
        </section>
      </div>
    </section>
  `
})
export class HomePageComponent {
  readonly inputModes = [
    {
      kicker: 'Paste',
      title: 'Text claims',
      detail: 'Tweets, captions, WhatsApp forwards, and suspicious headlines.'
    },
    {
      kicker: 'Fetch',
      title: 'Article URLs',
      detail: 'Pull readable page content before scoring credibility signals.'
    },
    {
      kicker: 'Inspect',
      title: 'Image uploads',
      detail: 'Screenshots, posters, memes, and edited visual claims.'
    },
    {
      kicker: 'Extract',
      title: 'PDF documents',
      detail: 'OCR-backed investigation for scanned notices and reports.'
    },
    {
      kicker: 'Track',
      title: 'Video URLs',
      detail: 'Investigate reels, YouTube links, and shared video sources.'
    },
    {
      kicker: 'Upload',
      title: 'Video clips',
      detail: 'Short exported files with media-aware limitations and checks.'
    }
  ];

  readonly heroStats = [
    {
      label: 'Inputs',
      value: '6 modes',
      detail: 'Text, article URL, image, PDF, video URL, and uploaded video.'
    },
    {
      label: 'Report',
      value: 'Claim-level',
      detail: 'Each report surfaces claims, evidence points, source URLs, and next steps.'
    },
    {
      label: 'Guardrail',
      value: 'Cannot analyze',
      detail: 'Inaccessible media is labeled clearly instead of forced into a fake score.'
    }
  ];

  readonly previewMetrics = [
    {
      label: 'Analysis status',
      value: 'Limited analysis',
      detail: 'Source metadata available, direct media needs deeper verification.'
    },
    {
      label: 'Source trail',
      value: '6 URLs captured',
      detail: 'Relevant links are preserved for the audit trail and export.'
    },
    {
      label: 'Language',
      value: 'Hindi + English mix',
      detail: 'Code-mixed wording can still be grouped into investigation-ready signals.'
    },
    {
      label: 'Follow-up',
      value: 'Ask the report',
      detail: 'Use ASI-1 again to clarify weak claims or strongest supporting source.'
    }
  ];

  readonly previewClaims = [
    {
      status: 'Questionable',
      confidence: 'Confidence 78',
      title: 'The reel claims every household can claim a 15,000 rupee subsidy through one shared link.',
      detail: 'No institution-backed source is visible and the wording pushes urgency over verifiable details.',
      tone: 'border-amber-300 bg-amber-300/10 text-amber-100'
    },
    {
      status: 'Needs source',
      confidence: 'Next step',
      title: 'The uploader identity and platform context require direct inspection before trust increases.',
      detail: 'The report recommends checking the original page, uploader history, and any official announcement URLs.',
      tone: 'border-sky-300 bg-sky-300/10 text-sky-100'
    }
  ];

  readonly bharatUseCases = [
    {
      tag: 'Forwarded as received',
      input: 'Text + screenshot',
      title: 'WhatsApp safety rumor',
      detail: 'Investigate school closure rumors, panic messages, and public-safety claims written in Hindi, Hinglish, or mixed scripts.'
    },
    {
      tag: 'Shared video link',
      input: 'Video URL',
      title: 'Political or subsidy reel',
      detail: 'Analyze the source trail around a viral clip and return cannot analyze when the media itself is unavailable.'
    },
    {
      tag: 'Scanned notice',
      input: 'PDF + OCR',
      title: 'Government-style circular or document',
      detail: 'Extract text from scanned files, inspect claim wording, and map institutions or missing attribution.'
    }
  ];

  readonly proofCards = [
    {
      kicker: 'Structured outputs',
      title: 'Schema-safe results from ASI-1',
      detail: 'The backend expects consistent JSON so claims, entities, status, and evidence can be rendered cleanly in the UI.',
      tone: 'text-sky-700'
    },
    {
      kicker: 'Multi-step reasoning',
      title: 'Signals first, verdict second',
      detail: 'The workflow extracts claims and context before assembling the final investigation result and reasoning.',
      tone: 'text-emerald-700'
    },
    {
      kicker: 'Human guardrails',
      title: 'Limitations stay visible',
      detail: 'The app shows what the model could not inspect, which sources were used, and what humans should verify next.',
      tone: 'text-amber-700'
    },
    {
      kicker: 'Interactive reports',
      title: 'The report can answer follow-up questions',
      detail: 'After the first run, the user can query the stored report for weaker claims, strongest sources, or next checks.',
      tone: 'text-rose-700'
    }
  ];

  readonly workflowSteps = [
    {
      step: 'Step 1',
      title: 'Ingest raw material',
      detail: 'Choose the right mode for text, article, image, PDF, video URL, or uploaded clip.'
    },
    {
      step: 'Step 2',
      title: 'Trace claims and signals',
      detail: 'ASI-1 extracts risk labels, entities, suspicious framing, media clues, and evidence-ready claims.'
    },
    {
      step: 'Step 3',
      title: 'Deliver a report people can inspect',
      detail: 'The result shows score or status, reasoning, citations, limitations, and recommended verification checks.'
    }
  ];

  readonly evidenceHighlights = [
    {
      title: 'Credibility score or explicit analysis status',
      badge: 'Score',
      detail: 'A missing or inaccessible video does not get mislabeled as fake just to fill the UI.'
    },
    {
      title: 'Claim-level evidence with confidence and source URLs',
      badge: 'Claims',
      detail: 'Every important statement can point to supporting or contradictory context.'
    },
    {
      title: 'Visited URLs and audit trail',
      badge: 'Trail',
      detail: 'The report preserves relevant source links so reviewers can inspect the path to the verdict.'
    },
    {
      title: 'Limitations and next verification steps',
      badge: 'Guardrails',
      detail: 'The product says what it knows, what it cannot know yet, and how a human should continue.'
    }
  ];
}
