import { CommonModule, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { finalize } from 'rxjs/operators';
import {
  analysisStatusLabel,
  AnalysisResult,
  claimAssessmentTone,
  FollowUpResponse,
  inputTypeLabel,
  scoreLabel,
  verdictTone
} from '../../core/models/analysis.model';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AnalysisStore } from '../../core/store/analysis.store';
import { ScoreGaugeComponent } from '../../shared/components/score-gauge.component';

@Component({
  selector: 'app-results-page',
  standalone: true,
  imports: [CommonModule, DatePipe, NgClass, RouterLink, ScoreGaugeComponent],
  styles: [`
    .result-meta-chip,
    .result-title,
    .result-link,
    .result-wrap {
      min-width: 0;
      overflow-wrap: anywhere;
      word-break: break-word;
    }
  `],
  template: `
    <section class="px-4 pb-24 pt-6 md:px-8 xl:px-10 2xl:px-12">
      <div class="mx-auto max-w-[1680px] space-y-10">
        <div class="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <span class="eyebrow">Investigation report</span>
            <h1 class="mt-5 text-5xl font-semibold leading-tight text-ink md:text-6xl">Evidence, claim-level citations, and credibility in one audit-ready report.</h1>
            <p class="mt-4 max-w-3xl text-lg leading-8 text-ink-soft">
              Review the AI verdict, inspect per-claim evidence, track source links, and see which checks still need a human follow-up before sharing the result.
            </p>
          </div>
          <div class="flex flex-wrap gap-3">
            <a routerLink="/analysis" class="rounded-full border border-ink/10 bg-white/80 px-5 py-3 text-sm font-semibold text-ink transition hover:border-ink/20 hover:bg-white">New investigation</a>
            <button type="button" (click)="exportReport()" [disabled]="!analysis()" class="rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white transition hover:bg-ink-soft disabled:cursor-not-allowed disabled:opacity-50">Export report</button>
          </div>
        </div>

        <div *ngIf="loading()" class="shell-card p-10 text-center text-sm text-ink-soft">Loading investigation report...</div>
        <div *ngIf="errorMessage()" class="rounded-[24px] border border-rose-200 bg-rose-50 p-5 text-rose-700">{{ errorMessage() }}</div>

        <ng-container *ngIf="analysis() as report; else emptyState">
          <div class="grid gap-6 lg:grid-cols-[0.92fr_1.08fr]">
            <app-score-gauge [score]="report.credibilityScore" [verdict]="report.verdict"></app-score-gauge>

            <div class="shell-card min-w-0 p-6">
              <div class="grid gap-3 sm:grid-cols-2 xl:grid-cols-3">
                <article class="result-meta-chip rounded-[20px] border px-4 py-4" [ngClass]="primaryBadgeTone(report)">
                  <p class="text-[10px] font-semibold uppercase tracking-[0.24em]">Verdict</p>
                  <p class="mt-2 text-sm font-semibold leading-6">{{ primaryBadgeLabel(report) }}</p>
                </article>
                <article class="result-meta-chip rounded-[20px] border border-slate-200 bg-slate-50 px-4 py-4">
                  <p class="text-[10px] font-semibold uppercase tracking-[0.24em] text-steel">Signal</p>
                  <p class="mt-2 text-sm font-semibold leading-6 text-ink">{{ scoreLabel(report.credibilityScore) }}</p>
                </article>
                <article class="result-meta-chip rounded-[20px] border border-slate-200 bg-white px-4 py-4">
                  <p class="text-[10px] font-semibold uppercase tracking-[0.24em] text-steel">Generated</p>
                  <p class="mt-2 text-sm font-semibold leading-6 text-ink">{{ report.createdAt | date: 'medium' }}</p>
                </article>
              </div>

              <div class="mt-4 flex flex-wrap items-start gap-3">
                <span class="result-meta-chip rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">{{ inputTypeLabel(report.inputType) }}</span>
                <span class="result-meta-chip rounded-full bg-sky-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">Model: {{ report.modelUsed }}</span>
                <span class="result-meta-chip rounded-full bg-emerald-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-emerald-700">{{ report.contentLanguage || 'Unknown language' }}</span>
                <span *ngIf="report.imageMimeType" class="result-meta-chip rounded-full bg-violet-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-violet-700">{{ report.imageMimeType }}</span>
                <span *ngIf="report.visitedUrls?.length" class="result-meta-chip rounded-full bg-amber-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-amber-700">{{ report.visitedUrls.length }} source{{ report.visitedUrls.length > 1 ? 's' : '' }} used</span>
              </div>

              <div class="mt-4 flex flex-wrap gap-2" *ngIf="report.riskLabels.length">
                <span *ngFor="let label of report.riskLabels" class="result-meta-chip rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-rose-700">
                  {{ label }}
                </span>
              </div>

              <div *ngIf="report.statusReason" class="mt-5 rounded-[20px] border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-950">
                <p class="font-semibold text-amber-900">{{ analysisStatusLabel(report.analysisStatus) }}</p>
                <p class="mt-2 leading-7">{{ report.statusReason }}</p>
              </div>

              <h2 class="result-title mt-5 text-3xl font-semibold leading-tight text-ink">{{ report.sourceTitle || report.sourceLabel || 'Manual submission' }}</h2>
              <a *ngIf="report.sourceUrl" [href]="report.sourceUrl" target="_blank" rel="noreferrer" class="result-link mt-3 block text-sm font-medium leading-7 text-ember underline decoration-ember/40 underline-offset-4">{{ report.sourceUrl }}</a>
              <p class="result-wrap mt-5 text-base leading-8 text-ink-soft">{{ report.summary }}</p>
            </div>
          </div>

          <div class="grid gap-6 lg:grid-cols-[1.02fr_0.98fr]">
            <div class="shell-card min-w-0 p-6">
              <div class="flex items-center justify-between gap-4">
                <h2 class="text-3xl font-semibold text-ink">Why the AI reached this verdict</h2>
                <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">Explainable AI</span>
              </div>
              <p class="result-wrap mt-5 text-base leading-8 text-ink-soft">{{ report.reasoning }}</p>
            </div>

            <div class="shell-card min-w-0 p-6">
              <div class="flex items-center justify-between gap-4">
                <h2 class="text-3xl font-semibold text-ink">What still needs human verification</h2>
                <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">Guardrails</span>
              </div>

              <div class="mt-5 grid gap-3 sm:grid-cols-2">
                <div class="soft-panel p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Limitations</p>
                  <div class="mt-3 space-y-2 text-sm leading-7 text-ink-soft" *ngIf="report.limitations.length; else noLimitations">
                    <p *ngFor="let limitation of report.limitations">- {{ limitation }}</p>
                  </div>
                  <ng-template #noLimitations>
                    <p class="mt-3 text-sm leading-7 text-ink-soft">No explicit limitations were returned for this report.</p>
                  </ng-template>
                </div>

                <div class="soft-panel p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Recommended checks</p>
                  <div class="mt-3 space-y-2 text-sm leading-7 text-ink-soft" *ngIf="report.recommendedChecks.length; else noChecks">
                    <p *ngFor="let check of report.recommendedChecks">- {{ check }}</p>
                  </div>
                  <ng-template #noChecks>
                    <p class="mt-3 text-sm leading-7 text-ink-soft">No follow-up checks were suggested for this report.</p>
                  </ng-template>
                </div>
              </div>
            </div>
          </div>

          <div class="shell-card min-w-0 p-6">
            <div class="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
              <div class="max-w-3xl">
                <h2 class="text-3xl font-semibold text-ink">Ask the report a follow-up question</h2>
                <p class="mt-3 text-sm leading-7 text-ink-soft">Use this during the demo to challenge a claim, ask for a simpler explanation, or request the next best verification step.</p>
              </div>
              <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">Interactive Q&amp;A</span>
            </div>

            <div class="mt-5 flex flex-wrap gap-2">
              <button
                *ngFor="let suggestion of followUpSuggestions"
                type="button"
                (click)="useSuggestedQuestion(suggestion)"
                class="rounded-full border border-slate-200 bg-white px-4 py-3 text-sm font-semibold text-ink transition hover:border-ink/15 hover:bg-slate-50"
              >
                {{ suggestion }}
              </button>
            </div>

            <div class="mt-5 flex flex-col gap-3 lg:flex-row">
              <input
                type="text"
                [value]="followUpQuestion()"
                (input)="updateFollowUpQuestion($event)"
                class="w-full rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-ink outline-none transition focus:border-ink"
                placeholder="Ask about the weakest claim, strongest source, or next verification step..."
              />
              <button
                type="button"
                (click)="askFollowUp()"
                [disabled]="followUpLoading() || !analysis()"
                class="rounded-full bg-ink px-6 py-4 text-sm font-semibold text-white transition hover:bg-ink-soft disabled:cursor-not-allowed disabled:opacity-60"
              >
                {{ followUpLoading() ? 'Asking ASI-1...' : 'Ask ASI-1' }}
              </button>
            </div>

            <div *ngIf="followUpError()" class="mt-4 rounded-[20px] border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {{ followUpError() }}
            </div>

            <div *ngIf="followUpResponse() as reply" class="mt-5 grid gap-4 lg:grid-cols-[1.02fr_0.98fr]">
              <div class="rounded-[24px] border border-slate-200 bg-slate-50 p-5">
                <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Answer</p>
                <p class="mt-3 text-sm leading-7 text-ink-soft">{{ reply.answer }}</p>
              </div>

              <div class="grid gap-4">
                <div class="rounded-[24px] border border-slate-200 bg-white p-5">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Relevant URLs</p>
                  <div class="mt-3 space-y-2 text-sm leading-7" *ngIf="reply.sourceUrls.length; else noFollowUpUrls">
                    <a *ngFor="let url of reply.sourceUrls" [href]="url" target="_blank" rel="noreferrer" class="block break-all font-medium text-ember underline decoration-ember/30 underline-offset-4">
                      {{ url }}
                    </a>
                  </div>
                  <ng-template #noFollowUpUrls>
                    <p class="mt-3 text-sm leading-7 text-ink-soft">No specific URLs were returned for this follow-up.</p>
                  </ng-template>
                </div>

                <div class="rounded-[24px] border border-slate-200 bg-white p-5">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Suggested checks</p>
                  <div class="mt-3 space-y-2 text-sm leading-7 text-ink-soft" *ngIf="reply.suggestedChecks.length; else noFollowUpChecks">
                    <p *ngFor="let check of reply.suggestedChecks">- {{ check }}</p>
                  </div>
                  <ng-template #noFollowUpChecks>
                    <p class="mt-3 text-sm leading-7 text-ink-soft">No extra checks were suggested for this follow-up.</p>
                  </ng-template>
                </div>
              </div>
            </div>
          </div>

          <div class="grid gap-6 lg:grid-cols-[1.02fr_0.98fr]">
            <div class="shell-card min-w-0 p-6">
              <div class="flex items-center justify-between gap-4">
                <h2 class="text-3xl font-semibold text-ink">Sources used by the AI</h2>
                <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">Audit trail</span>
              </div>
              <p class="mt-3 text-sm leading-7 text-ink-soft">These URLs were captured from the user input, model output, or claim-level evidence produced during the investigation.</p>

              <div class="mt-5 grid gap-3 sm:grid-cols-3">
                <div class="soft-panel p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Recorded links</p>
                  <p class="mt-3 text-3xl font-semibold text-ink">{{ report.visitedUrls.length || 0 }}</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">URLs visible in the investigation trail.</p>
                </div>
                <div class="soft-panel p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Input basis</p>
                  <p class="mt-3 text-lg font-semibold text-ink">{{ sourceBasisLabel(report) }}</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">What the system primarily relied on before generating the verdict.</p>
                </div>
                <div class="soft-panel p-4">
                  <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Verified claims</p>
                  <p class="mt-3 text-lg font-semibold text-ink">{{ report.claims.length }}</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">Claims listed with evidence, confidence, and follow-up steps.</p>
                </div>
              </div>

              <div class="mt-6 space-y-3" *ngIf="report.visitedUrls?.length; else noVisitedUrls">
                <article *ngFor="let link of report.visitedUrls; let i = index" class="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-4">
                  <div class="flex items-center justify-between gap-3">
                    <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">Source {{ i + 1 }}</p>
                    <span class="rounded-full bg-white px-3 py-1 text-[10px] font-semibold uppercase tracking-[0.2em] text-steel">{{ domainFromUrl(link) }}</span>
                  </div>
                  <a [href]="link" target="_blank" rel="noreferrer" class="mt-2 block break-all text-sm font-medium leading-7 text-ember underline decoration-ember/30 underline-offset-4">
                    {{ link }}
                  </a>
                </article>
              </div>
              <ng-template #noVisitedUrls>
                <div class="mt-6 rounded-[20px] border border-slate-200 bg-slate-50 px-5 py-5 text-sm text-ink-soft">
                  <p class="font-semibold text-ink">No recorded source links for this report.</p>
                  <p class="mt-2 leading-7">
                    This report appears to be based mainly on {{ sourceBasisLabel(report).toLowerCase() }}. The backend now supports claim-level source URLs, but they depend on what the model can confidently cite.
                  </p>
                </div>
              </ng-template>
            </div>

            <div class="shell-card min-w-0 p-6">
              <h2 class="text-3xl font-semibold text-ink">Detected entities</h2>
              <div class="mt-5 flex flex-wrap gap-3" *ngIf="report.entities.length; else noEntities">
                <article *ngFor="let entity of report.entities" class="min-w-[220px] flex-1 rounded-[22px] border border-slate-200 bg-slate-50 p-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">{{ entity.type }}</p>
                  <p class="mt-2 text-lg font-semibold text-ink">{{ entity.name }}</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">{{ entity.context }}</p>
                </article>
              </div>
              <ng-template #noEntities>
                <p class="mt-5 text-sm text-ink-soft">No key entities were extracted from this submission.</p>
              </ng-template>
            </div>
          </div>

          <div class="shell-card min-w-0 p-6">
            <div class="flex items-center justify-between gap-4">
              <h2 class="text-3xl font-semibold text-ink">Claim-by-claim evidence</h2>
              <span class="rounded-full bg-white px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">{{ report.claims.length }} claims</span>
            </div>

            <div class="mt-6 grid gap-4 xl:grid-cols-2" *ngIf="report.claims.length; else noClaims">
              <article *ngFor="let claim of report.claims; let i = index" class="rounded-[26px] border border-slate-200 bg-white p-5 shadow-[0_10px_30px_rgba(15,27,45,0.05)]">
                <div class="flex flex-wrap items-center gap-3">
                  <p class="text-xs font-semibold uppercase tracking-[0.28em] text-steel">Claim {{ i + 1 }}</p>
                  <span class="rounded-full border px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em]" [ngClass]="claimBadge(claim.assessment)">
                    {{ claim.assessment }}
                  </span>
                  <span class="rounded-full bg-slate-100 px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-steel">
                    Confidence {{ claim.confidence }}/100
                  </span>
                </div>

                <h3 class="result-wrap mt-3 text-xl font-semibold text-ink">{{ claim.statement }}</h3>

                <div class="mt-4 rounded-[18px] bg-amber-50 p-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-amber-700">Suspicion note</p>
                  <p class="mt-2 text-sm leading-7 text-amber-950">{{ claim.suspicion }}</p>
                </div>

                <div class="mt-4 rounded-[18px] bg-slate-50 p-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Evidence hint</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">{{ claim.evidenceHint }}</p>
                </div>

                <div class="mt-4 rounded-[18px] bg-emerald-50 p-4" *ngIf="claim.evidencePoints.length; else noEvidencePoints">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-emerald-700">Evidence points</p>
                  <div class="mt-2 space-y-2 text-sm leading-7 text-emerald-950">
                    <p *ngFor="let point of claim.evidencePoints">- {{ point }}</p>
                  </div>
                </div>
                <ng-template #noEvidencePoints>
                  <div class="mt-4 rounded-[18px] bg-slate-50 p-4 text-sm leading-7 text-ink-soft">
                    No explicit evidence bullets were returned for this claim.
                  </div>
                </ng-template>

                <div class="mt-4" *ngIf="claim.sourceUrls.length">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Claim sources</p>
                  <div class="mt-3 space-y-2">
                    <a *ngFor="let sourceUrl of claim.sourceUrls" [href]="sourceUrl" target="_blank" rel="noreferrer" class="block break-all rounded-[16px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium leading-7 text-ember underline decoration-ember/30 underline-offset-4">
                      {{ sourceUrl }}
                    </a>
                  </div>
                </div>

                <div class="mt-4 rounded-[18px] border border-slate-200 bg-white px-4 py-4">
                  <p class="text-xs font-semibold uppercase tracking-[0.24em] text-steel">Recommended next step</p>
                  <p class="mt-2 text-sm leading-7 text-ink-soft">{{ claim.nextStep }}</p>
                </div>
              </article>
            </div>
            <ng-template #noClaims>
              <p class="mt-5 text-sm text-ink-soft">No claim list was returned for this investigation.</p>
            </ng-template>
          </div>
        </ng-container>

        <ng-template #emptyState>
          <div *ngIf="!loading() && !errorMessage()" class="shell-card p-10 text-center">
            <h2 class="text-3xl font-semibold text-ink">No report loaded yet.</h2>
            <p class="mx-auto mt-4 max-w-xl text-base leading-8 text-ink-soft">Run an investigation from the analysis page or open one from the archive to review text, image, PDF, or video evidence.</p>
            <a routerLink="/analysis" class="mt-6 inline-flex rounded-full bg-ember px-6 py-3 text-sm font-semibold text-white transition hover:bg-[#cc4d3a]">Go to investigation</a>
          </div>
        </ng-template>
      </div>
    </section>
  `
})
export class ResultsPageComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(AnalysisApiService);
  private readonly store = inject(AnalysisStore);
  private readonly destroyRef = inject(DestroyRef);

  readonly analysis = signal<AnalysisResult | null>(this.store.latestAnalysis());
  readonly loading = signal(false);
  readonly errorMessage = signal('');
  readonly followUpQuestion = signal('');
  readonly followUpLoading = signal(false);
  readonly followUpError = signal('');
  readonly followUpResponse = signal<FollowUpResponse | null>(null);
  readonly followUpSuggestions = [
    'What is the weakest claim in this report?',
    'Which source should a human verify next?',
    'Explain the verdict in simpler words.'
  ];

  protected readonly scoreLabel = scoreLabel;
  protected readonly inputTypeLabel = inputTypeLabel;
  protected readonly analysisStatusLabel = analysisStatusLabel;

  constructor() {
    this.route.paramMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const id = params.get('id');
        if (!id) {
          this.analysis.set(this.store.latestAnalysis());
          return;
        }

        this.loading.set(true);
        this.errorMessage.set('');
        this.api.getAnalysis(id)
          .pipe(
            finalize(() => this.loading.set(false)),
            takeUntilDestroyed(this.destroyRef)
          )
          .subscribe({
            next: (result) => {
              this.analysis.set(result);
              this.store.setLatestAnalysis(result);
              this.followUpResponse.set(null);
              this.followUpError.set('');
            },
            error: (error: HttpErrorResponse) => {
              this.errorMessage.set(error.error?.message ?? 'This investigation could not be loaded.');
              this.analysis.set(null);
              this.followUpResponse.set(null);
            }
          });
      });
  }

  verdictBadge(verdict: AnalysisResult['verdict']) {
    return verdictTone(verdict);
  }

  primaryBadgeTone(report: AnalysisResult) {
    if (report.analysisStatus === 'CANNOT_ANALYZE') {
      return 'bg-slate-100 text-slate-700 border-slate-200';
    }
    if (report.analysisStatus === 'LIMITED') {
      return 'bg-amber-100 text-amber-700 border-amber-200';
    }
    return verdictTone(report.verdict);
  }

  primaryBadgeLabel(report: AnalysisResult) {
    if (report.analysisStatus !== 'COMPLETED') {
      return analysisStatusLabel(report.analysisStatus);
    }
    return this.videoVerdictLabel(report);
  }

  claimBadge(assessment: string) {
    return claimAssessmentTone(assessment);
  }

  videoVerdictLabel(report: AnalysisResult) {
    if (!report.verdict) {
      return 'No final verdict';
    }
    if (report.inputType === 'VIDEO') {
      switch (report.verdict) {
        case 'Verified':
          return 'Video appears reliable';
        case 'Likely True':
          return 'Video likely authentic';
        case 'Questionable':
          return 'Video needs verification';
        default:
          return 'Video likely misleading';
      }
    }
    return report.verdict;
  }

  exportReport() {
    const report = this.analysis();
    if (!report) {
      return;
    }

    const content = [
      `# AI Internet Detective Report`,
      ``,
      `Verdict: ${this.videoVerdictLabel(report)}`,
      `Analysis status: ${analysisStatusLabel(report.analysisStatus)}`,
      `Status reason: ${report.statusReason ?? 'N/A'}`,
      `Credibility score: ${report.credibilityScore === null ? 'N/A' : `${report.credibilityScore}/100`}`,
      `Input type: ${inputTypeLabel(report.inputType)}`,
      `Model used: ${report.modelUsed}`,
      `Content language: ${report.contentLanguage || 'Unknown'}`,
      `Risk labels: ${report.riskLabels.length ? report.riskLabels.join(', ') : 'None'}`,
      `Generated: ${new Date(report.createdAt).toLocaleString()}`,
      `Source title: ${report.sourceTitle ?? report.sourceLabel ?? 'Manual submission'}`,
      `Source URL: ${report.sourceUrl ?? 'N/A'}`,
      `Image MIME type: ${report.imageMimeType ?? 'N/A'}`,
      ``,
      `## Sources used by the AI`,
      ...(report.visitedUrls?.length ? report.visitedUrls.map((url, index) => `${index + 1}. ${url}`) : ['None captured']),
      ``,
      `## Summary`,
      report.summary,
      ``,
      `## Reasoning`,
      report.reasoning,
      ``,
      `## Limitations`,
      ...(report.limitations.length ? report.limitations.map((limitation) => `- ${limitation}`) : ['- None returned']),
      ``,
      `## Recommended Checks`,
      ...(report.recommendedChecks.length ? report.recommendedChecks.map((check) => `- ${check}`) : ['- None returned']),
      ``,
      `## Claims`,
      ...report.claims.map((claim, index) => [
        `${index + 1}. ${claim.statement}`,
        `   - Assessment: ${claim.assessment}`,
        `   - Confidence: ${claim.confidence}/100`,
        `   - Suspicion: ${claim.suspicion}`,
        `   - Evidence hint: ${claim.evidenceHint}`,
        `   - Evidence points: ${claim.evidencePoints.length ? claim.evidencePoints.join(' | ') : 'None returned'}`,
        `   - Source URLs: ${claim.sourceUrls.length ? claim.sourceUrls.join(', ') : 'None returned'}`,
        `   - Next step: ${claim.nextStep}`
      ].join('\n')),
      ``,
      `## Entities`,
      ...report.entities.map((entity) => `- ${entity.name} (${entity.type}): ${entity.context}`)
    ].join('\n');

    const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `investigation-${report.id}.md`;
    anchor.click();
    URL.revokeObjectURL(url);
  }

  updateFollowUpQuestion(event: Event) {
    const input = event.target as HTMLInputElement | null;
    this.followUpQuestion.set(input?.value ?? '');
  }

  useSuggestedQuestion(question: string) {
    this.followUpQuestion.set(question);
    this.askFollowUp();
  }

  askFollowUp() {
    const report = this.analysis();
    const question = this.followUpQuestion().trim();
    if (!report || !question) {
      this.followUpError.set('Enter a follow-up question before asking ASI-1.');
      return;
    }

    this.followUpLoading.set(true);
    this.followUpError.set('');
    this.api.askFollowUp(report.id, question)
      .pipe(
        finalize(() => this.followUpLoading.set(false)),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (reply) => {
          this.followUpResponse.set(reply);
        },
        error: (error: HttpErrorResponse) => {
          this.followUpError.set(error.error?.message ?? 'The follow-up question could not be answered right now.');
        }
      });
  }

  sourceBasisLabel(report: AnalysisResult) {
    switch (report.inputType) {
      case 'URL':
        return 'Fetched article content';
      case 'IMAGE':
        return 'Image evidence';
      case 'PDF':
        return 'Document and OCR text';
      case 'VIDEO':
        return 'Video metadata and source context';
      default:
        return 'Submitted text only';
    }
  }

  domainFromUrl(url: string) {
    try {
      return new URL(url).hostname.replace(/^www\./, '');
    } catch {
      return 'source';
    }
  }
}
