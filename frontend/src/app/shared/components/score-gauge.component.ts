import { CommonModule } from '@angular/common';
import { Component, computed, input } from '@angular/core';
import { scoreLabel, scoreTone, Verdict } from '../../core/models/analysis.model';

@Component({
  selector: 'app-score-gauge',
  standalone: true,
  imports: [CommonModule],
  host: {
    class: 'block min-w-0'
  },
  template: `
    <div class="shell-card p-6">
      <div class="flex flex-col gap-6 sm:flex-row sm:items-start sm:justify-between">
        <div class="min-w-0">
          <p class="text-xs font-semibold uppercase tracking-[0.3em] text-steel">Credibility score</p>
          <h3 class="mt-2 text-3xl font-semibold text-ink">{{ scoreDisplay() }}</h3>
          <p class="mt-2 max-w-xs break-words text-sm leading-7 text-ink-soft">{{ scoreLabelText() }}</p>
          <div class="mt-5 inline-flex max-w-full break-words rounded-full bg-slate-100 px-4 py-2 text-xs font-semibold uppercase tracking-[0.22em] text-steel">
            {{ verdict() || 'No final verdict' }}
          </div>
        </div>
        <div
          class="relative mx-auto grid h-32 w-32 shrink-0 place-items-center rounded-full border border-white/80 shadow-[0_18px_40px_rgba(15,27,45,0.08)] sm:mx-0"
          [style.background]="gaugeBackground()"
        >
          <div class="grid h-[96px] w-[96px] place-items-center rounded-full bg-white text-center shadow-inner">
            <div>
              <span class="text-3xl font-semibold text-ink">{{ scoreCenter() }}</span>
              <p class="mt-1 text-[10px] font-semibold uppercase tracking-[0.24em] text-steel">Score</p>
            </div>
          </div>
        </div>
      </div>

      <div class="mt-6">
        <div class="score-bar">
          <div class="h-full rounded-full bg-gradient-to-r transition-all duration-700" [ngClass]="scoreGradient()" [style.width.%]="score()"></div>
        </div>
      </div>
    </div>
  `
})
export class ScoreGaugeComponent {
  readonly score = input<number | null>(null);
  readonly verdict = input<Verdict | null>(null);

  protected readonly scoreGradient = computed(() => scoreTone(this.score()));
  protected readonly scoreLabelText = computed(() => scoreLabel(this.score()));
  protected readonly scoreDisplay = computed(() => this.score() === null || this.score() === undefined ? 'N/A' : `${this.score()}/100`);
  protected readonly scoreCenter = computed(() => this.score() === null || this.score() === undefined ? 'N/A' : `${this.score()}`);
  protected readonly gaugeBackground = computed(() => {
    const value = this.score() ?? 0;
    return `conic-gradient(#0f1b2d 0deg, #0f1b2d ${value * 3.6}deg, rgba(107,122,144,0.15) ${value * 3.6}deg 360deg)`;
  });
}
