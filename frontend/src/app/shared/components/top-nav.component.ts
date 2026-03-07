import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-top-nav',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <header class="sticky top-0 z-50 px-4 py-4 md:px-8 xl:px-10 2xl:px-12">
      <div class="mx-auto flex max-w-[1680px] items-center justify-between rounded-full border border-white/70 bg-white/75 px-5 py-3 shadow-[0_18px_50px_rgba(15,27,45,0.12)] backdrop-blur md:px-7 xl:px-8">
        <a routerLink="/" class="flex items-center gap-3 text-sm font-semibold tracking-[0.28em] text-ink">
          <span class="flex h-11 w-11 items-center justify-center rounded-full bg-ink text-sm text-white">AI</span>
          <span class="hidden md:inline">INTERNET DETECTIVE</span>
        </a>

        <nav class="flex items-center gap-2 text-sm font-medium text-ink-soft md:gap-3">
          <a routerLink="/" routerLinkActive="bg-ink text-white" [routerLinkActiveOptions]="{ exact: true }" class="rounded-full px-4 py-2 transition hover:bg-slate-100">Home</a>
          <a routerLink="/analysis" routerLinkActive="bg-ink text-white" class="rounded-full px-4 py-2 transition hover:bg-slate-100">Investigate</a>
          <a routerLink="/results" routerLinkActive="bg-ink text-white" class="rounded-full px-4 py-2 transition hover:bg-slate-100">Report</a>
          <a routerLink="/history" routerLinkActive="bg-ink text-white" class="rounded-full px-4 py-2 transition hover:bg-slate-100">Archive</a>
        </nav>
      </div>
    </header>
  `
})
export class TopNavComponent {}
