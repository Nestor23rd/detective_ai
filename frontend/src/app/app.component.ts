import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TopNavComponent } from './shared/components/top-nav.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, TopNavComponent],
  template: `
    <div class="min-h-screen">
      <app-top-nav></app-top-nav>
      <main>
        <router-outlet></router-outlet>
      </main>
      <footer class="px-4 pb-12 md:px-8 xl:px-10 2xl:px-12">
        <div class="mx-auto max-w-[1680px] rounded-[32px] border border-white/70 bg-white/76 px-6 py-7 text-sm text-ink-soft backdrop-blur lg:px-8 xl:px-10">
          <div class="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div class="max-w-3xl">
              <p class="font-semibold text-ink">AI Internet Detective</p>
              <p class="mt-2 leading-7">A polished hackathon build for credibility analysis across text, URLs, screenshots, PDFs, and multilingual forwards with claim-level evidence workflows.</p>
            </div>
            <div class="text-left md:text-right">
              <p class="font-semibold uppercase tracking-[0.2em] text-steel">Hackathon build</p>
              <p class="mt-2">Angular · Spring Boot · MongoDB · ASI-1</p>
            </div>
          </div>
        </div>
      </footer>
    </div>
  `
})
export class AppComponent {}
