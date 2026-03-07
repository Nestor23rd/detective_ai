import { CommonModule, DatePipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AnalysisStore } from '../../core/store/analysis.store';
import { analysisStatusLabel, HistoryItem, inputTypeLabel, scoreTone, verdictTone } from '../../core/models/analysis.model';

@Component({
  selector: 'app-history-page',
  standalone: true,
  imports: [CommonModule, DatePipe, NgClass, RouterLink],
  styles: [`
    .history-wrap {
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
            <span class="eyebrow">Case archive</span>
            <h1 class="mt-5 text-5xl font-semibold leading-tight text-ink md:text-6xl">Track every investigation with risk labels, language clues, and stored reports.</h1>
            <p class="mt-4 max-w-3xl text-lg leading-8 text-ink-soft">This archive turns the backend database into a demo-friendly newsroom log for articles, screenshots, OCR documents, suspicious claims, and media-rich reports.</p>
          </div>
          <a routerLink="/analysis" class="inline-flex w-fit rounded-full bg-ink px-6 py-3 text-sm font-semibold text-white transition hover:bg-ink-soft">Start new investigation</a>
        </div>

        <div class="shell-card p-5">
          <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
            <div class="max-w-2xl">
              <p class="text-xs font-semibold uppercase tracking-[0.28em] text-steel">Archive filters</p>
              <p class="mt-2 text-sm leading-7 text-ink-soft">Search by title, summary, language, risk label, or verdict to find the strongest demo reports quickly.</p>
            </div>
            <input
              type="search"
              [value]="query()"
              (input)="updateQuery($event)"
              class="w-full rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-ink outline-none transition focus:border-ink lg:max-w-md"
              placeholder="Search by verdict, risk label, title, or language..."
            />
          </div>
        </div>

        <div *ngIf="loading()" class="shell-card p-10 text-center text-sm text-ink-soft">Loading archived investigations...</div>
        <div *ngIf="errorMessage()" class="rounded-[24px] border border-rose-200 bg-rose-50 p-5 text-rose-700">{{ errorMessage() }}</div>

        <div class="grid gap-5" *ngIf="filteredHistory().length; else emptyState">
          <article *ngFor="let item of filteredHistory()" class="shell-card min-w-0 p-6">
            <div class="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
              <div class="history-wrap min-w-0 max-w-3xl flex-1">
                <div class="flex flex-wrap items-start gap-3">
                  <span class="history-wrap rounded-full border px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em]" [ngClass]="statusBadge(item)">
                    {{ item.analysisStatus === 'COMPLETED' ? (item.verdict || 'No verdict') : analysisStatusLabel(item.analysisStatus) }}
                  </span>
                  <span class="history-wrap text-sm text-steel">{{ item.createdAt | date: 'medium' }}</span>
                  <span class="history-wrap text-sm text-steel">{{ inputTypeLabel(item.inputType) }}</span>
                  <span *ngIf="item.modelUsed" class="history-wrap rounded-full bg-sky-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-sky-700">{{ item.modelUsed }}</span>
                  <span *ngIf="item.contentLanguage" class="history-wrap rounded-full bg-emerald-50 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-emerald-700">{{ item.contentLanguage }}</span>
                </div>

                <div class="mt-4 flex flex-wrap gap-2" *ngIf="item.riskLabels.length">
                  <span *ngFor="let label of item.riskLabels" class="history-wrap rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-rose-700">
                    {{ label }}
                  </span>
                </div>

                <h2 class="history-wrap mt-4 text-3xl font-semibold leading-tight text-ink">{{ item.sourceTitle || item.sourceLabel || 'Manual submission' }}</h2>
                <p *ngIf="item.statusReason" class="history-wrap mt-4 rounded-[18px] border border-amber-200 bg-amber-50 px-4 py-3 text-sm leading-7 text-amber-950">{{ item.statusReason }}</p>
                <p class="history-wrap mt-4 text-base leading-8 text-ink-soft">{{ item.summary }}</p>
                <a *ngIf="item.sourceUrl" [href]="item.sourceUrl" target="_blank" rel="noreferrer" class="history-wrap mt-3 block text-sm font-medium leading-7 text-ember underline decoration-ember/40 underline-offset-4">{{ item.sourceUrl }}</a>
              </div>

              <div class="min-w-0 rounded-[28px] bg-slate-50 p-5 lg:w-[320px] lg:min-w-[320px]">
                <div class="flex flex-wrap items-end justify-between gap-4">
                  <div class="min-w-0">
                    <p class="text-xs font-semibold uppercase tracking-[0.28em] text-steel">Credibility</p>
                    <p class="mt-2 text-4xl font-semibold text-ink">{{ item.credibilityScore === null ? 'N/A' : item.credibilityScore }}</p>
                  </div>
                  <a [routerLink]="['/results', item.id]" class="shrink-0 rounded-full bg-ember px-4 py-2 text-sm font-semibold text-white transition hover:bg-[#cc4d3a]">Open report</a>
                </div>
                <div class="mt-4 score-bar">
                  <div class="h-full rounded-full bg-gradient-to-r" [ngClass]="scoreBar(item.credibilityScore)" [style.width.%]="item.credibilityScore ?? 0"></div>
                </div>
              </div>
            </div>
          </article>
        </div>

        <ng-template #emptyState>
          <div *ngIf="!loading() && !errorMessage()" class="shell-card p-10 text-center">
            <h2 class="text-3xl font-semibold text-ink">{{ history().length ? 'No investigations match this search.' : 'No investigations stored yet.' }}</h2>
            <p class="mx-auto mt-4 max-w-xl text-base leading-8 text-ink-soft">
              {{ history().length ? 'Try a broader keyword, verdict, or risk label.' : 'Run the first investigation and it will appear here automatically.' }}
            </p>
          </div>
        </ng-template>
      </div>
    </section>
  `
})
export class HistoryPageComponent {
  private readonly api = inject(AnalysisApiService);
  private readonly store = inject(AnalysisStore);
  private readonly destroyRef = inject(DestroyRef);

  readonly history = this.store.history;
  readonly loading = signal(true);
  readonly errorMessage = signal('');
  readonly query = signal('');
  readonly filteredHistory = computed(() => {
    const term = this.query().trim().toLowerCase();
    if (!term) {
      return this.history();
    }

    return this.history().filter((item) => {
      const haystack = [
        item.verdict,
        item.summary,
        item.sourceTitle,
        item.sourceLabel,
        item.sourceUrl,
        item.modelUsed,
        item.contentLanguage,
        ...item.riskLabels
      ]
        .filter((value): value is string => !!value)
        .join(' ')
        .toLowerCase();

      return haystack.includes(term);
    });
  });
  protected readonly inputTypeLabel = inputTypeLabel;
  protected readonly analysisStatusLabel = analysisStatusLabel;

  constructor() {
    this.api.getHistory()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (items) => {
          this.store.setHistory(items);
          this.loading.set(false);
        },
        error: (error: HttpErrorResponse) => {
          this.loading.set(false);
          this.errorMessage.set(error.error?.message ?? 'The archive is unavailable right now.');
        }
      });
  }

  verdictBadge(verdict: HistoryItem['verdict']) {
    return verdictTone(verdict);
  }

  statusBadge(item: HistoryItem) {
    if (item.analysisStatus === 'CANNOT_ANALYZE') {
      return 'bg-slate-100 text-slate-700 border-slate-200';
    }
    if (item.analysisStatus === 'LIMITED') {
      return 'bg-amber-100 text-amber-700 border-amber-200';
    }
    return verdictTone(item.verdict);
  }

  scoreBar(score: number | null) {
    return scoreTone(score);
  }

  updateQuery(event: Event) {
    const input = event.target as HTMLInputElement | null;
    this.query.set(input?.value ?? '');
  }
}
