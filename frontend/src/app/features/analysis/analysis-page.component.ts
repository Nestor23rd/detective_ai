import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnDestroy, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AnalysisApiService } from '../../core/services/analysis-api.service';
import { AnalysisStore } from '../../core/store/analysis.store';

@Component({
  selector: 'app-analysis-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  styles: [`
    .analysis-live-shell {
      position: relative;
      overflow: hidden;
    }

    .analysis-live-shell::before {
      content: '';
      position: absolute;
      inset: 0;
      background:
        radial-gradient(circle at 18% 18%, rgba(56, 189, 248, 0.24), transparent 20%),
        radial-gradient(circle at 82% 24%, rgba(167, 139, 250, 0.18), transparent 22%),
        radial-gradient(circle at 52% 88%, rgba(34, 197, 94, 0.14), transparent 22%);
      pointer-events: none;
    }

    .analysis-live-shell::after {
      content: '';
      position: absolute;
      inset: -40% -10%;
      background: linear-gradient(115deg, transparent 0%, rgba(255, 255, 255, 0.42) 48%, transparent 58%);
      transform: translateX(-30%);
      animation: analysis-sweep 6s linear infinite;
      pointer-events: none;
    }

    .analysis-grid {
      position: absolute;
      inset: 0;
      background-image:
        linear-gradient(rgba(14, 116, 144, 0.08) 1px, transparent 1px),
        linear-gradient(90deg, rgba(14, 116, 144, 0.08) 1px, transparent 1px);
      background-size: 26px 26px;
      mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.85), transparent 100%);
      pointer-events: none;
    }

    .analysis-orb {
      position: absolute;
      border-radius: 9999px;
      filter: blur(2px);
      opacity: 0.9;
      pointer-events: none;
    }

    .analysis-orb-primary {
      top: -4rem;
      right: -2rem;
      height: 12rem;
      width: 12rem;
      background: radial-gradient(circle, rgba(59, 130, 246, 0.42) 0%, rgba(59, 130, 246, 0) 72%);
      animation: analysis-float 6s ease-in-out infinite;
    }

    .analysis-orb-secondary {
      bottom: -5rem;
      left: -1rem;
      height: 11rem;
      width: 11rem;
      background: radial-gradient(circle, rgba(168, 85, 247, 0.24) 0%, rgba(168, 85, 247, 0) 72%);
      animation: analysis-float 7.5s ease-in-out infinite reverse;
    }

    .analysis-core {
      position: relative;
      display: grid;
      height: 7.75rem;
      width: 7.75rem;
      place-items: center;
      border-radius: 9999px;
      background: radial-gradient(circle at center, rgba(255, 255, 255, 0.96), rgba(226, 232, 240, 0.82));
      box-shadow:
        0 18px 48px rgba(14, 116, 144, 0.18),
        inset 0 0 0 1px rgba(255, 255, 255, 0.9);
    }

    .analysis-core::before,
    .analysis-core::after {
      content: '';
      position: absolute;
      border-radius: 9999px;
      border: 1px solid rgba(14, 165, 233, 0.24);
    }

    .analysis-core::before {
      inset: -0.85rem;
      animation: analysis-orbit 6s linear infinite;
    }

    .analysis-core::after {
      inset: -1.6rem;
      border-color: rgba(99, 102, 241, 0.18);
      animation: analysis-orbit 8s linear infinite reverse;
    }

    .analysis-core-pulse {
      position: absolute;
      inset: 0.8rem;
      border-radius: 9999px;
      background: radial-gradient(circle, rgba(14, 165, 233, 0.22), rgba(99, 102, 241, 0.06));
      animation: analysis-pulse 2.2s ease-in-out infinite;
    }

    .analysis-core-dot {
      position: absolute;
      top: -0.25rem;
      left: 50%;
      height: 0.75rem;
      width: 0.75rem;
      border-radius: 9999px;
      background: linear-gradient(135deg, #38bdf8, #6366f1);
      box-shadow: 0 0 0 6px rgba(56, 189, 248, 0.12);
      transform-origin: 0 4.1rem;
      animation: analysis-spin 5.6s linear infinite;
    }

    .analysis-core-bars {
      display: flex;
      align-items: flex-end;
      gap: 0.22rem;
      height: 2rem;
    }

    .analysis-core-bars span {
      width: 0.32rem;
      border-radius: 9999px;
      background: linear-gradient(180deg, #0ea5e9, #6366f1);
      animation: analysis-bars 1.1s ease-in-out infinite;
      transform-origin: bottom center;
    }

    .analysis-core-bars span:nth-child(1) { height: 0.8rem; animation-delay: 0s; }
    .analysis-core-bars span:nth-child(2) { height: 1.3rem; animation-delay: 0.14s; }
    .analysis-core-bars span:nth-child(3) { height: 1.85rem; animation-delay: 0.28s; }
    .analysis-core-bars span:nth-child(4) { height: 1.1rem; animation-delay: 0.42s; }
    .analysis-core-bars span:nth-child(5) { height: 1.55rem; animation-delay: 0.56s; }

    .analysis-log-row {
      position: relative;
      overflow: hidden;
    }

    .analysis-log-row::before {
      content: '';
      position: absolute;
      inset: 0;
      background: linear-gradient(90deg, rgba(14, 165, 233, 0.06), transparent 75%);
      opacity: 0;
      transition: opacity 180ms ease;
    }

    .analysis-log-row.is-active::before,
    .analysis-log-row.is-complete::before {
      opacity: 1;
    }

    .analysis-log-dot {
      position: relative;
      height: 0.85rem;
      width: 0.85rem;
      border-radius: 9999px;
      flex-shrink: 0;
    }

    .analysis-log-dot::after {
      content: '';
      position: absolute;
      inset: -0.2rem;
      border-radius: 9999px;
      border: 1px solid currentColor;
      opacity: 0.22;
    }

    .analysis-data-chip {
      position: relative;
      overflow: hidden;
    }

    .analysis-data-chip::after {
      content: '';
      position: absolute;
      inset: auto 0 0 0;
      height: 2px;
      background: linear-gradient(90deg, rgba(14, 165, 233, 0.1), rgba(99, 102, 241, 0.75), rgba(14, 165, 233, 0.1));
      animation: analysis-line 1.8s linear infinite;
    }

    @keyframes analysis-sweep {
      from { transform: translateX(-35%); }
      to { transform: translateX(110%); }
    }

    @keyframes analysis-float {
      0%, 100% { transform: translate3d(0, 0, 0); }
      50% { transform: translate3d(0, 14px, 0); }
    }

    @keyframes analysis-pulse {
      0%, 100% { transform: scale(0.92); opacity: 0.52; }
      50% { transform: scale(1.08); opacity: 0.9; }
    }

    @keyframes analysis-orbit {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    @keyframes analysis-spin {
      from { transform: rotate(0deg); }
      to { transform: rotate(360deg); }
    }

    @keyframes analysis-bars {
      0%, 100% { transform: scaleY(0.68); opacity: 0.55; }
      50% { transform: scaleY(1.16); opacity: 1; }
    }

    @keyframes analysis-line {
      from { transform: translateX(-100%); }
      to { transform: translateX(100%); }
    }
  `],
  template: `
    <section class="px-4 pb-24 pt-6 md:px-8 xl:px-10 2xl:px-12">
      <div class="mx-auto grid max-w-[1680px] gap-10 xl:grid-cols-[0.88fr_1.12fr] 2xl:gap-12">
        <div class="space-y-6">
          <div>
            <span class="eyebrow">Analysis console</span>
            <h1 class="mt-5 text-5xl font-semibold leading-tight text-ink md:text-6xl">Investigate a story, a screenshot, a PDF, or a WhatsApp-style forward.</h1>
            <p class="mt-5 max-w-xl text-lg leading-8 text-ink-soft">
              Use the AI like a newsroom fact-checking desk: send it raw content, screenshots, posters, articles, or code-mixed Hindi and Hinglish claims, then inspect the claim-level evidence and credibility signals it surfaces.
            </p>
          </div>

          <div class="shell-card p-6">
            <h2 class="text-2xl font-semibold text-ink">What gets checked</h2>
            <div class="mt-5 grid gap-4 md:grid-cols-2">
              <div class="rounded-[22px] bg-slate-50 p-4">
                <p class="text-sm font-semibold text-ink">Misinformation patterns</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">Sensational phrasing, false certainty, conspiracy markers, and missing sourcing.</p>
              </div>
              <div class="rounded-[22px] bg-slate-50 p-4">
                <p class="text-sm font-semibold text-ink">Fact extraction</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">Named people, organizations, places, concepts, and statistics that deserve scrutiny.</p>
              </div>
              <div class="rounded-[22px] bg-slate-50 p-4">
                <p class="text-sm font-semibold text-ink">Visual inspection</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">Screenshots, social cards, flyers, and suspicious image-based claims can now be analyzed too.</p>
              </div>
              <div class="rounded-[22px] bg-slate-50 p-4">
                <p class="text-sm font-semibold text-ink">Multilingual context</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">English, Hindi, Hinglish, and code-mixed forwards can be grouped into risk labels and investigation-ready claims.</p>
              </div>
            </div>
          </div>

          <div class="shell-card p-6">
            <div class="flex items-center justify-between gap-4">
              <div>
                <h2 class="text-2xl font-semibold text-ink">Demo-ready sample cases</h2>
                <p class="mt-2 text-sm leading-7 text-ink-soft">Load a quick example to show the claim extraction, multilingual handling, and evidence workflow without typing from scratch.</p>
              </div>
              <span class="rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">3 scenarios</span>
            </div>

            <div class="mt-5 grid gap-4">
              <button
                *ngFor="let sample of sampleInvestigations"
                type="button"
                (click)="loadSample(sample.text)"
                class="rounded-[24px] border border-slate-200 bg-slate-50 p-5 text-left transition hover:-translate-y-0.5 hover:border-ink/15 hover:bg-white"
              >
                <div class="flex flex-wrap items-center gap-3">
                  <span class="rounded-full bg-white px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em] text-steel">{{ sample.tag }}</span>
                  <span class="text-xs font-semibold uppercase tracking-[0.18em] text-ember">Load into text mode</span>
                </div>
                <p class="mt-3 text-lg font-semibold text-ink">{{ sample.title }}</p>
                <p class="mt-2 text-sm leading-7 text-ink-soft">{{ sample.text }}</p>
              </button>
            </div>
          </div>
        </div>

        <div class="shell-card p-6 md:p-8">
          <div class="flex flex-wrap gap-3">
            <button type="button" (click)="switchMode('text')" [ngClass]="mode() === 'text' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Paste text
            </button>
            <button type="button" (click)="switchMode('url')" [ngClass]="mode() === 'url' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Analyze URL
            </button>
            <button type="button" (click)="switchMode('image')" [ngClass]="mode() === 'image' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Analyze image
            </button>
            <button type="button" (click)="switchMode('document')" [ngClass]="mode() === 'document' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Analyze PDF
            </button>
            <button type="button" (click)="switchMode('video-file')" [ngClass]="mode() === 'video-file' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Upload video
            </button>
            <button type="button" (click)="switchMode('video-url')" [ngClass]="mode() === 'video-url' ? 'bg-ink text-white' : 'bg-slate-100 text-ink'" class="rounded-full px-5 py-3 text-sm font-semibold transition">
              Video URL
            </button>
          </div>

          <div *ngIf="submitting()" class="analysis-live-shell mt-6 rounded-[32px] border border-sky-200/80 bg-[linear-gradient(135deg,rgba(239,246,255,0.94)_0%,rgba(248,250,252,0.97)_46%,rgba(238,242,255,0.98)_100%)] text-sm text-sky-950 shadow-[0_28px_80px_rgba(14,116,217,0.14)]">
            <div class="analysis-grid"></div>
            <div class="analysis-orb analysis-orb-primary"></div>
            <div class="analysis-orb analysis-orb-secondary"></div>

            <div class="relative grid gap-6 p-6 lg:grid-cols-[1.08fr_0.92fr] lg:p-7">
              <div class="space-y-5">
                <div class="flex items-start justify-between gap-5">
                  <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.32em] text-sky-700">Live ASI-1 processing</p>
                    <h3 class="mt-2 text-[1.75rem] font-semibold leading-tight text-ink">{{ activeModelLabel() }} is processing your {{ modeLabel() }}.</h3>
                    <p class="mt-3 max-w-2xl leading-7 text-sky-900/90">{{ analysisStageDescriptions[activeStageIndex()] }}</p>
                  </div>

                  <div class="analysis-core shrink-0">
                    <div class="analysis-core-pulse"></div>
                    <div class="analysis-core-dot"></div>
                    <div class="relative z-[1] text-center">
                      <p class="text-[10px] font-semibold uppercase tracking-[0.28em] text-sky-700">ASI Core</p>
                      <div class="analysis-core-bars mt-3">
                        <span></span>
                        <span></span>
                        <span></span>
                        <span></span>
                        <span></span>
                      </div>
                    </div>
                  </div>
                </div>

                <div class="grid gap-3 sm:grid-cols-3">
                  <article *ngFor="let chip of analysisDataChips; let i = index" class="analysis-data-chip rounded-[22px] border border-white/80 bg-white/70 px-4 py-4 shadow-[0_12px_30px_rgba(15,27,45,0.05)] backdrop-blur">
                    <p class="text-[11px] font-semibold uppercase tracking-[0.24em] text-steel">{{ chip.label }}</p>
                    <p class="mt-2 text-base font-semibold text-ink">{{ chipValue(i) }}</p>
                    <p class="mt-1 text-xs leading-6 text-sky-800/80">{{ chip.detail }}</p>
                  </article>
                </div>

                <div class="grid gap-3 md:grid-cols-3">
                  <div *ngFor="let stage of analysisStages; let i = index" class="rounded-[24px] border px-4 py-4 transition" [ngClass]="stageTone(i)">
                    <div class="flex items-center gap-3">
                      <span class="inline-flex h-9 w-9 items-center justify-center rounded-full text-xs font-semibold"
                        [ngClass]="i < activeStageIndex() ? 'bg-emerald-100 text-emerald-700' : i === activeStageIndex() ? 'bg-sky-100 text-sky-700 animate-pulse' : 'bg-slate-100 text-slate-500'">
                        {{ i + 1 }}
                      </span>
                      <div>
                        <p class="text-sm font-semibold text-ink">{{ stage }}</p>
                        <p class="mt-1 text-xs leading-6" [ngClass]="i <= activeStageIndex() ? 'text-sky-800' : 'text-slate-500'">
                          {{ analysisStageHints[i] }}
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div class="grid gap-4">
                <section class="rounded-[26px] border border-white/80 bg-white/72 p-5 shadow-[0_14px_34px_rgba(15,27,45,0.06)] backdrop-blur">
                  <div class="flex items-center justify-between gap-4">
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-sky-700">Pipeline status</p>
                      <p class="mt-2 text-lg font-semibold text-ink">{{ loadingCaption() }}</p>
                    </div>
                    <div class="rounded-full bg-sky-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.24em] text-sky-700">
                      {{ progress() }}%
                    </div>
                  </div>

                  <div class="mt-5 h-3 overflow-hidden rounded-full bg-sky-100/80">
                    <div class="h-full rounded-full bg-gradient-to-r from-sky-500 via-cyan-400 to-violet-500 transition-all duration-700" [style.width.%]="progress()"></div>
                  </div>

                  <div class="mt-4 flex flex-wrap gap-2">
                    <span *ngFor="let chip of analysisSignalBadges; let i = index" class="rounded-full px-3 py-2 text-[11px] font-semibold uppercase tracking-[0.22em]" [ngClass]="signalBadgeTone(i)">
                      {{ chip }}: {{ signalBadgeValue(i) }}
                    </span>
                  </div>
                </section>

                <section class="rounded-[26px] border border-white/80 bg-[#07142a]/95 p-5 text-white shadow-[0_20px_40px_rgba(4,17,36,0.25)]">
                  <div class="flex items-center justify-between gap-3">
                    <div>
                      <p class="text-xs font-semibold uppercase tracking-[0.28em] text-sky-200">Live activity stream</p>
                      <p class="mt-2 text-sm leading-7 text-sky-100/80">A demo-friendly feed showing how the report is assembled in real time.</p>
                    </div>
                    <span class="rounded-full border border-sky-400/25 bg-sky-400/10 px-3 py-2 text-[10px] font-semibold uppercase tracking-[0.26em] text-sky-200">Realtime</span>
                  </div>

                  <div class="mt-5 space-y-3">
                    <article *ngFor="let item of analysisLiveFeed; let i = index" class="analysis-log-row rounded-[22px] border px-4 py-4 transition" [ngClass]="activityTone(i)">
                      <div class="relative z-[1] flex gap-3">
                        <span class="analysis-log-dot" [ngClass]="activityDotTone(i)"></span>
                        <div>
                          <div class="flex flex-wrap items-center gap-2">
                            <p class="text-sm font-semibold">{{ item.title }}</p>
                            <span class="rounded-full px-2 py-1 text-[10px] font-semibold uppercase tracking-[0.24em]" [ngClass]="activityStateBadge(i)">
                              {{ activityStateLabel(i) }}
                            </span>
                          </div>
                          <p class="mt-2 text-xs leading-6 text-slate-300">{{ item.detail }}</p>
                        </div>
                      </div>
                    </article>
                  </div>
                </section>
              </div>
            </div>
          </div>

          <form class="mt-6 space-y-5" [formGroup]="form" (ngSubmit)="submit()">
            <div *ngIf="mode() === 'text'; else nonTextMode">
              <label class="mb-2 block text-sm font-semibold text-ink" for="content">Content to investigate</label>
              <textarea
                id="content"
                formControlName="text"
                rows="12"
                class="w-full rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm leading-7 text-ink outline-none transition focus:border-ink"
                placeholder="Paste a tweet, article, paragraph, or headline here..."
              ></textarea>
              <p class="mt-2 text-xs tracking-[0.2em] text-steel uppercase">Best for headlines, social posts, copied article text, or suspicious claims.</p>
            </div>

            <ng-template #nonTextMode>
              <div *ngIf="mode() === 'url'; else fileModes">
                <label class="mb-2 block text-sm font-semibold text-ink" for="url">Article URL</label>
                <input
                  id="url"
                  type="url"
                  formControlName="url"
                  class="w-full rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-ink outline-none transition focus:border-ink"
                  placeholder="https://example.com/news-story"
                />
                <p class="mt-2 text-xs tracking-[0.2em] text-steel uppercase">The backend fetches the page and extracts readable article paragraphs.</p>
              </div>

              <ng-template #fileModes>
                <div *ngIf="mode() === 'image'; else documentOrVideoMode">
                  <label class="mb-2 block text-sm font-semibold text-ink" for="image">Screenshot or suspicious image</label>
                  <div
                    class="rounded-[24px] border-2 border-dashed px-5 py-6 transition"
                    [ngClass]="draggingImage() ? 'border-sky-400 bg-sky-50' : 'border-slate-300 bg-slate-50'"
                    (dragover)="onDragOver($event)"
                    (dragleave)="onDragLeave($event)"
                    (drop)="onDropImage($event)"
                  >
                    <div class="flex flex-col items-center justify-center text-center">
                      <p class="text-base font-semibold text-ink">Drop an image here</p>
                      <p class="mt-2 text-sm leading-7 text-ink-soft">or choose a file from your device to investigate screenshots, posters, flyers, or visual claims.</p>
                      <label for="image" class="mt-4 inline-flex cursor-pointer rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white transition hover:bg-ink-soft">
                        Browse image
                      </label>
                    </div>
                    <input
                      id="image"
                      type="file"
                      accept="image/png,image/jpeg,image/jpg,image/webp,image/gif"
                      (change)="onImageSelected($event)"
                      class="sr-only"
                    />
                  </div>
                  <p class="mt-2 text-xs tracking-[0.2em] text-steel uppercase">Upload or drop a screenshot, poster, meme, flyer, or image-based claim. Max size: 5 MB.</p>

                  <div *ngIf="imagePreviewUrl()" class="mt-5 overflow-hidden rounded-[24px] border border-slate-200 bg-white">
                    <img [src]="imagePreviewUrl()!" [alt]="selectedImageName() || 'Selected image preview'" class="max-h-[340px] w-full object-contain bg-slate-100" />
                    <div class="flex items-center justify-between gap-3 px-4 py-3 text-sm text-ink-soft">
                      <span>{{ selectedImageName() || 'Selected image' }}</span>
                      <span>{{ selectedImageType() || 'Unknown type' }}</span>
                    </div>
                  </div>
                </div>

                <ng-template #documentOrVideoMode>
                  <div *ngIf="mode() === 'document'; else videoFileMode">
                    <label class="mb-2 block text-sm font-semibold text-ink" for="document">PDF or scanned document</label>
                    <div
                      class="rounded-[24px] border-2 border-dashed px-5 py-6 transition"
                      [ngClass]="draggingDocument() ? 'border-violet-400 bg-violet-50' : 'border-slate-300 bg-slate-50'"
                      (dragover)="onDocumentDragOver($event)"
                      (dragleave)="onDocumentDragLeave($event)"
                      (drop)="onDropDocument($event)"
                    >
                      <div class="flex flex-col items-center justify-center text-center">
                        <p class="text-base font-semibold text-ink">Drop a PDF here</p>
                        <p class="mt-2 text-sm leading-7 text-ink-soft">Use this mode for reports, statements, scanned documents, or exported screenshots packed into a PDF.</p>
                        <label for="document" class="mt-4 inline-flex cursor-pointer rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white transition hover:bg-ink-soft">
                          Browse document
                        </label>
                      </div>
                      <input
                        id="document"
                        type="file"
                        accept="application/pdf"
                        (change)="onDocumentSelected($event)"
                        class="sr-only"
                      />
                    </div>
                    <label class="mt-4 flex items-center gap-3 text-sm text-ink-soft">
                      <input type="checkbox" class="h-4 w-4 rounded border-slate-300" [checked]="ocrEnabled()" (change)="toggleOcr($event)" />
                      Enable OCR fallback for scanned PDFs and image-based text.
                    </label>
                    <div *ngIf="selectedDocumentName()" class="mt-4 rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-ink-soft">
                      <span class="font-semibold text-ink">Selected document:</span> {{ selectedDocumentName() }}
                    </div>
                  </div>

                  <ng-template #videoFileMode>
                    <div>
                      <div *ngIf="mode() === 'video-url'; else videoUploadMode">
                        <label class="mb-2 block text-sm font-semibold text-ink" for="videoUrl">Video URL</label>
                        <input
                          id="videoUrl"
                          type="url"
                          formControlName="videoUrl"
                          class="w-full rounded-[24px] border border-slate-200 bg-slate-50 px-5 py-4 text-sm text-ink outline-none transition focus:border-ink"
                          placeholder="https://example.com/clip"
                        />
                        <p class="mt-2 text-xs tracking-[0.2em] text-steel uppercase">Best for suspicious reels, shared clips, platform links, or video sources that need context-aware investigation.</p>
                      </div>

                      <ng-template #videoUploadMode>
                        <label class="mb-2 block text-sm font-semibold text-ink" for="videoFile">Video file</label>
                        <div
                          class="rounded-[24px] border-2 border-dashed px-5 py-6 transition"
                          [ngClass]="draggingVideo() ? 'border-rose-400 bg-rose-50' : 'border-slate-300 bg-slate-50'"
                          (dragover)="onVideoDragOver($event)"
                          (dragleave)="onVideoDragLeave($event)"
                          (drop)="onDropVideo($event)"
                        >
                          <div class="flex flex-col items-center justify-center text-center">
                            <p class="text-base font-semibold text-ink">Drop a video here</p>
                            <p class="mt-2 text-sm leading-7 text-ink-soft">Use this mode for short clips, screen recordings, or exported videos when you want the system to inspect available file metadata and context.</p>
                            <label for="videoFile" class="mt-4 inline-flex cursor-pointer rounded-full bg-ink px-5 py-3 text-sm font-semibold text-white transition hover:bg-ink-soft">
                              Browse video
                            </label>
                          </div>
                          <input
                            id="videoFile"
                            type="file"
                            accept="video/*"
                            (change)="onVideoSelected($event)"
                            class="sr-only"
                          />
                        </div>
                        <div *ngIf="selectedVideoName()" class="mt-4 rounded-[20px] border border-slate-200 bg-white px-4 py-3 text-sm text-ink-soft">
                          <span class="font-semibold text-ink">Selected video:</span> {{ selectedVideoName() }}
                        </div>
                      </ng-template>
                    </div>
                  </ng-template>
                </ng-template>
              </ng-template>
            </ng-template>

            <div *ngIf="errorMessage()" class="rounded-[20px] border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-700">
              {{ errorMessage() }}
            </div>

            <button
              type="submit"
              [disabled]="submitting()"
              class="inline-flex w-full items-center justify-center rounded-full bg-ember px-6 py-4 text-sm font-semibold text-white transition hover:-translate-y-0.5 hover:bg-[#cc4d3a] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {{ submitting() ? 'AI is investigating…' : 'Run AI Investigation' }}
            </button>
          </form>
        </div>
      </div>
    </section>
  `
})
export class AnalysisPageComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly api = inject(AnalysisApiService);
  private readonly store = inject(AnalysisStore);
  private readonly destroyRef = inject(DestroyRef);

  readonly mode = signal<'text' | 'url' | 'image' | 'document' | 'video-file' | 'video-url'>('text');
  readonly submitting = signal(false);
  readonly errorMessage = signal('');
  readonly imagePreviewUrl = signal<string | null>(null);
  readonly selectedImageName = signal('');
  readonly selectedImageType = signal('');
  readonly draggingImage = signal(false);
  readonly maxImageSizeBytes = 5 * 1024 * 1024;

  readonly draggingDocument = signal(false);
  readonly selectedDocumentName = signal('');
  readonly ocrEnabled = signal(true);
  readonly maxDocumentSizeBytes = 8 * 1024 * 1024;
  readonly selectedUploadFile = signal<File | null>(null);

  readonly draggingVideo = signal(false);
  readonly selectedVideoName = signal('');
  readonly maxVideoSizeBytes = 5 * 1024 * 1024;

  readonly progress = signal(0);
  readonly activeStageIndex = signal(0);
  readonly loadingCaption = signal('Preparing evidence map');
  readonly analysisStages = [
    'Extracting evidence',
    'Checking red flags',
    'Building the verdict'
  ];
  readonly analysisStageHints = [
    'Pulling out claims, entities, screenshots, and context cues.',
    'Looking for manipulation patterns, missing sourcing, and credibility signals.',
    'Scoring trustworthiness and preparing the final report.'
  ];
  readonly analysisStageDescriptions = [
    'The system is reading the submission and isolating the most relevant facts and visual clues.',
    'The model is comparing what it sees against credibility red flags and suspicious framing patterns.',
    'The final explanation is being assembled so the verdict stays transparent and demo-ready.'
  ];
  readonly analysisDataChips = [
    {
      label: 'Structured output',
      detail: 'Schema-safe JSON so the final report stays renderable.'
    },
    {
      label: 'Source trace',
      detail: 'URLs, metadata, and explicit evidence are being captured.'
    },
    {
      label: 'Claim graph',
      detail: 'Claims, entities, and contradictions are linked step by step.'
    }
  ];
  readonly analysisSignalBadges = ['web scan', 'evidence map', 'guardrails'];
  readonly analysisLiveFeed = [
    {
      title: 'Normalizing submission',
      detail: 'Cleaning the payload, trimming noise, and mapping the selected analysis mode.'
    },
    {
      title: 'Extracting claims and entities',
      detail: 'Pulling out the people, organizations, places, and statements worth verifying.'
    },
    {
      title: 'Capturing source and metadata clues',
      detail: 'Inspecting URLs, file hints, host reputation signals, and context breadcrumbs.'
    },
    {
      title: 'Scoring confidence and contradictions',
      detail: 'Balancing supporting signals against missing sourcing, suspicious framing, and uncertainty.'
    },
    {
      title: 'Assembling an explainable report',
      detail: 'Packaging the verdict, evidence, and follow-up checks for the results page.'
    }
  ];
  readonly sampleInvestigations = [
    {
      tag: 'Hinglish',
      title: 'WhatsApp forward about public safety',
      text: 'Forwarded as received: Kal se government bol rahi hai ki sab schools 10 din ke liye band honge because of toxic rain. Share this with every parent right now.'
    },
    {
      tag: 'Hindi',
      title: 'Viral subsidy announcement',
      text: 'यह संदेश तेजी से फैल रहा है: सरकार हर नागरिक को 15,000 रुपये की बिजली सब्सिडी दे रही है। लिंक पर क्लिक करके आज ही आवेदन करें।'
    },
    {
      tag: 'English',
      title: 'Manipulated science headline',
      text: 'Breaking: scientists confirm that ordinary phone chargers can detect cancer at home with 99.99% accuracy, but hospitals do not want this released.'
    }
  ];

  private progressIntervalId: ReturnType<typeof setInterval> | null = null;
  private stageIntervalId: ReturnType<typeof setInterval> | null = null;
  private feedIntervalId: ReturnType<typeof setInterval> | null = null;
  readonly activeFeedIndex = signal(0);

  readonly activeModelLabel = () => this.formatModelLabel(this.store.activeModel());

  readonly form = this.fb.group({
    text: ['', [Validators.required, Validators.maxLength(12000)]],
    url: [''],
    imageBase64: [''],
    imageMimeType: [''],
    imageName: [''],
    documentName: [''],
    videoUrl: ['']
  });

  ngOnDestroy() {
    this.stopLoadingExperience();
  }

  switchMode(mode: 'text' | 'url' | 'image' | 'document' | 'video-file' | 'video-url') {
    this.mode.set(mode);
    this.errorMessage.set('');

    if (mode === 'text') {
      this.form.patchValue({ url: '', imageBase64: '', imageMimeType: '', imageName: '', documentName: '', videoUrl: '' });
      this.selectedUploadFile.set(null);
      this.selectedDocumentName.set('');
      this.selectedVideoName.set('');
      this.form.controls.text.setValidators([Validators.required, Validators.maxLength(12000)]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([]);
    } else if (mode === 'url') {
      this.form.patchValue({ text: '', imageBase64: '', imageMimeType: '', imageName: '', documentName: '', videoUrl: '' });
      this.selectedUploadFile.set(null);
      this.selectedDocumentName.set('');
      this.selectedVideoName.set('');
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([Validators.required, Validators.pattern(/^https?:\/\/.+/i)]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([]);
    } else if (mode === 'image') {
      this.form.patchValue({ text: '', url: '', documentName: '', videoUrl: '' });
      this.selectedUploadFile.set(null);
      this.selectedDocumentName.set('');
      this.selectedVideoName.set('');
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([Validators.required]);
      this.form.controls.imageMimeType.setValidators([Validators.required]);
      this.form.controls.videoUrl.setValidators([]);
    } else if (mode === 'document') {
      this.form.patchValue({ text: '', url: '', imageBase64: '', imageMimeType: '', imageName: '', videoUrl: '' });
      this.imagePreviewUrl.set(null);
      this.selectedImageName.set('');
      this.selectedImageType.set('');
      this.selectedVideoName.set('');
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([]);
    } else if (mode === 'video-url') {
      this.form.patchValue({ text: '', url: '', imageBase64: '', imageMimeType: '', imageName: '', documentName: '' });
      this.imagePreviewUrl.set(null);
      this.selectedImageName.set('');
      this.selectedImageType.set('');
      this.selectedDocumentName.set('');
      this.selectedUploadFile.set(null);
      this.selectedVideoName.set('');
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([Validators.required, Validators.pattern(/^https?:\/\/.+/i)]);
    } else {
      this.form.patchValue({ text: '', url: '', imageBase64: '', imageMimeType: '', imageName: '', documentName: '', videoUrl: '' });
      this.imagePreviewUrl.set(null);
      this.selectedImageName.set('');
      this.selectedImageType.set('');
      this.selectedDocumentName.set('');
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([]);
    }

    this.form.controls.text.updateValueAndValidity();
    this.form.controls.url.updateValueAndValidity();
    this.form.controls.imageBase64.updateValueAndValidity();
    this.form.controls.imageMimeType.updateValueAndValidity();
    this.form.controls.videoUrl.updateValueAndValidity();
  }

  onImageSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.loadImageFile(file);
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.draggingImage.set(true);
  }

  onDragLeave(event: DragEvent) {
    event.preventDefault();
    this.draggingImage.set(false);
  }

  onDropImage(event: DragEvent) {
    event.preventDefault();
    this.draggingImage.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (!file) {
      return;
    }

    this.loadImageFile(file);
  }

  private loadImageFile(file: File) {
    if (!file.type.startsWith('image/')) {
      this.errorMessage.set('Please choose a supported image file.');
      return;
    }

    if (file.size > this.maxImageSizeBytes) {
      this.errorMessage.set('Please choose an image smaller than 5 MB.');
      return;
    }

    this.errorMessage.set('');
    this.selectedImageName.set(file.name);
    this.selectedImageType.set(file.type);
    this.switchMode('image');

    const reader = new FileReader();
    reader.onload = () => {
      const result = typeof reader.result === 'string' ? reader.result : '';
      const [header, base64 = ''] = result.split(',', 2);
      this.imagePreviewUrl.set(result || null);
      this.form.patchValue({
        imageBase64: base64,
        imageMimeType: file.type,
        imageName: file.name
      });
      if (!header.includes('base64') || !base64) {
        this.errorMessage.set('The selected image could not be encoded for analysis.');
      }
    };
    reader.readAsDataURL(file);
  }

  onDocumentSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.loadDocumentFile(file);
  }

  onDocumentDragOver(event: DragEvent) {
    event.preventDefault();
    this.draggingDocument.set(true);
  }

  onDocumentDragLeave(event: DragEvent) {
    event.preventDefault();
    this.draggingDocument.set(false);
  }

  onDropDocument(event: DragEvent) {
    event.preventDefault();
    this.draggingDocument.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (!file) {
      return;
    }
    this.loadDocumentFile(file);
  }

  toggleOcr(event: Event) {
    const input = event.target as HTMLInputElement;
    this.ocrEnabled.set(input.checked);
  }

  private loadDocumentFile(file: File) {
    if (file.type !== 'application/pdf' && !file.name.toLowerCase().endsWith('.pdf')) {
      this.errorMessage.set('Please choose a PDF document.');
      return;
    }
    if (file.size > this.maxDocumentSizeBytes) {
      this.errorMessage.set('Please choose a document smaller than 8 MB.');
      return;
    }

    this.errorMessage.set('');
    this.switchMode('document');
    this.selectedUploadFile.set(file);
    this.selectedDocumentName.set(file.name);
    this.form.patchValue({ documentName: file.name });
  }

  onVideoSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }
    this.loadVideoFile(file);
  }

  onVideoDragOver(event: DragEvent) {
    event.preventDefault();
    this.draggingVideo.set(true);
  }

  onVideoDragLeave(event: DragEvent) {
    event.preventDefault();
    this.draggingVideo.set(false);
  }

  onDropVideo(event: DragEvent) {
    event.preventDefault();
    this.draggingVideo.set(false);
    const file = event.dataTransfer?.files?.[0];
    if (!file) {
      return;
    }
    this.loadVideoFile(file);
  }

  private loadVideoFile(file: File) {
    if (!file.type.startsWith('video/')) {
      this.errorMessage.set('Please choose a supported video file.');
      return;
    }
    if (file.size > this.maxVideoSizeBytes) {
      this.errorMessage.set('Please choose a video smaller than 5 MB.');
      return;
    }

    this.errorMessage.set('');
    this.switchMode('video-file');
    this.selectedUploadFile.set(file);
    this.selectedVideoName.set(file.name);
  }

  private formatModelLabel(model: string) {
    const normalized = (model || 'asi1').trim().toLowerCase();
    if (normalized.includes('vision')) {
      return 'ASI-1 Vision';
    }
    if (normalized === 'asi1' || normalized.startsWith('asi1')) {
      return 'ASI-1 Fact-Check Model';
    }
    return model;
  }

  modeLabel() {
    switch (this.mode()) {
      case 'url':
        return 'URL';
      case 'image':
        return 'image';
      case 'document':
        return 'document';
      case 'video-file':
        return 'video';
      case 'video-url':
        return 'video URL';
      default:
        return 'text';
    }
  }

  stageTone(index: number) {
    if (index < this.activeStageIndex()) {
      return 'border-emerald-200 bg-emerald-50';
    }
    if (index === this.activeStageIndex()) {
      return 'border-sky-300 bg-white';
    }
    return 'border-slate-200 bg-slate-50';
  }

  chipValue(index: number) {
    const stage = this.activeStageIndex();
    const values = [
      ['locking schema', 'validating fields', 'report ready'],
      ['mapping input trail', 'capturing evidence URLs', 'audit trail sealed'],
      ['isolating claims', 'cross-linking entities', 'verdict graph complete']
    ];
    return values[index]?.[stage] ?? 'processing';
  }

  signalBadgeTone(index: number) {
    if (index === this.activeStageIndex()) {
      return 'bg-sky-100 text-sky-700';
    }
    if (index < this.activeStageIndex()) {
      return 'bg-emerald-100 text-emerald-700';
    }
    return 'bg-slate-100 text-slate-600';
  }

  signalBadgeValue(index: number) {
    if (index < this.activeStageIndex()) {
      return 'stable';
    }
    if (index === this.activeStageIndex()) {
      return 'live';
    }
    return 'queued';
  }

  activityTone(index: number) {
    if (index < this.activeFeedIndex()) {
      return 'is-complete border-emerald-400/20 bg-emerald-400/8';
    }
    if (index === this.activeFeedIndex()) {
      return 'is-active border-sky-400/35 bg-sky-400/12';
    }
    return 'border-white/10 bg-white/5';
  }

  activityDotTone(index: number) {
    if (index < this.activeFeedIndex()) {
      return 'bg-emerald-300 text-emerald-300';
    }
    if (index === this.activeFeedIndex()) {
      return 'bg-sky-300 text-sky-300 animate-pulse';
    }
    return 'bg-slate-500 text-slate-500';
  }

  activityStateBadge(index: number) {
    if (index < this.activeFeedIndex()) {
      return 'bg-emerald-400/15 text-emerald-200';
    }
    if (index === this.activeFeedIndex()) {
      return 'bg-sky-400/15 text-sky-200';
    }
    return 'bg-slate-400/10 text-slate-300';
  }

  activityStateLabel(index: number) {
    if (index < this.activeFeedIndex()) {
      return 'done';
    }
    if (index === this.activeFeedIndex()) {
      return 'live';
    }
    return 'queued';
  }

  private startLoadingExperience() {
    this.stopLoadingExperience();
    this.progress.set(8);
    this.activeStageIndex.set(0);
    this.activeFeedIndex.set(0);
    this.loadingCaption.set('Preparing evidence map');

    this.progressIntervalId = setInterval(() => {
      const current = this.progress();
      if (current >= 92) {
        return;
      }
      const increment = current < 35 ? 9 : current < 65 ? 6 : 3;
      this.progress.set(Math.min(92, current + increment));
    }, 650);

    this.stageIntervalId = setInterval(() => {
      const next = (this.activeStageIndex() + 1) % this.analysisStages.length;
      this.activeStageIndex.set(next);
      this.loadingCaption.set(next === 0 ? 'Preparing evidence map' : next === 1 ? 'Scanning credibility signals' : 'Finalizing report');
    }, 1900);

    this.feedIntervalId = setInterval(() => {
      const next = this.activeFeedIndex() >= this.analysisLiveFeed.length - 1 ? 0 : this.activeFeedIndex() + 1;
      this.activeFeedIndex.set(next);
    }, 1200);
  }

  private completeLoadingExperience() {
    this.progress.set(100);
    this.loadingCaption.set('Investigation complete');
    this.activeStageIndex.set(this.analysisStages.length - 1);
    this.activeFeedIndex.set(this.analysisLiveFeed.length - 1);
    this.stopLoadingExperience();
  }

  private stopLoadingExperience() {
    if (this.progressIntervalId) {
      clearInterval(this.progressIntervalId);
      this.progressIntervalId = null;
    }
    if (this.stageIntervalId) {
      clearInterval(this.stageIntervalId);
      this.stageIntervalId = null;
    }
    if (this.feedIntervalId) {
      clearInterval(this.feedIntervalId);
      this.feedIntervalId = null;
    }
  }

  loadSample(text: string) {
    this.switchMode('text');
    this.form.patchValue({ text, url: '', imageBase64: '', imageMimeType: '', imageName: '', documentName: '' });
    this.errorMessage.set('');
  }

  submit() {
    this.errorMessage.set('');

    if (this.mode() === 'text') {
      this.form.controls.text.setValidators([Validators.required, Validators.maxLength(12000)]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
    } else if (this.mode() === 'url') {
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([Validators.required, Validators.pattern(/^https?:\/\/.+/i)]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
    } else if (this.mode() === 'image') {
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([Validators.required]);
      this.form.controls.imageMimeType.setValidators([Validators.required]);
      this.form.controls.videoUrl.setValidators([]);
    } else if (this.mode() === 'video-url') {
      this.form.controls.text.setValidators([]);
      this.form.controls.url.setValidators([]);
      this.form.controls.imageBase64.setValidators([]);
      this.form.controls.imageMimeType.setValidators([]);
      this.form.controls.videoUrl.setValidators([Validators.required, Validators.pattern(/^https?:\/\/.+/i)]);
    } else if ((this.mode() === 'document' || this.mode() === 'video-file') && !this.selectedUploadFile()) {
      this.errorMessage.set(this.mode() === 'video-file' ? 'Please choose a video before starting the investigation.' : 'Please choose a PDF before starting the investigation.');
      return;
    }

    this.form.controls.text.updateValueAndValidity();
    this.form.controls.url.updateValueAndValidity();
    this.form.controls.imageBase64.updateValueAndValidity();
    this.form.controls.imageMimeType.updateValueAndValidity();
    this.form.controls.videoUrl.updateValueAndValidity();

    if (this.mode() !== 'document' && this.mode() !== 'video-file' && this.form.invalid) {
      this.form.markAllAsTouched();
      this.errorMessage.set('Please provide valid content before starting the investigation.');
      return;
    }

    const payload = this.mode() === 'text'
      ? { text: this.form.controls.text.value?.trim() ?? '' }
      : this.mode() === 'url'
        ? { url: this.form.controls.url.value?.trim() ?? '' }
        : this.mode() === 'image'
          ? {
              imageBase64: this.form.controls.imageBase64.value ?? '',
              imageMimeType: this.form.controls.imageMimeType.value ?? '',
              imageName: this.form.controls.imageName.value?.trim() ?? 'uploaded-image'
            }
          : this.mode() === 'video-url'
            ? { videoUrl: this.form.controls.videoUrl.value?.trim() ?? '' }
          : null;

    this.submitting.set(true);
    this.startLoadingExperience();
    this.store.startAnalysis(
      this.mode() === 'document'
        ? 'asi1-ocr'
        : this.mode() === 'video-file' || this.mode() === 'video-url'
          ? 'asi1-video'
          : 'asi1'
    );

    const request$ = this.mode() === 'document' || this.mode() === 'video-file'
      ? this.api.analyzeUpload({ file: this.selectedUploadFile()!, ocrEnabled: this.ocrEnabled() })
      : this.api.analyze(payload!);

    request$
      .pipe(
        finalize(() => {
          this.submitting.set(false);
          this.store.finishAnalysis();
          this.stopLoadingExperience();
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe({
        next: (result) => {
          this.store.setLatestAnalysis(result);
          this.store.startAnalysis(result.modelUsed || 'asi1');
          this.completeLoadingExperience();
          this.store.finishAnalysis();
          this.router.navigate(['/results', result.id]);
        },
        error: (error: HttpErrorResponse) => {
          this.errorMessage.set(error.error?.message ?? 'The analysis could not be completed right now.');
        }
      });
  }
}
