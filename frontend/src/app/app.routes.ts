import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home-page.component').then((m) => m.HomePageComponent)
  },
  {
    path: 'analysis',
    loadComponent: () => import('./features/analysis/analysis-page.component').then((m) => m.AnalysisPageComponent)
  },
  {
    path: 'results',
    loadComponent: () => import('./features/results/results-page.component').then((m) => m.ResultsPageComponent)
  },
  {
    path: 'results/:id',
    loadComponent: () => import('./features/results/results-page.component').then((m) => m.ResultsPageComponent)
  },
  {
    path: 'history',
    loadComponent: () => import('./features/history/history-page.component').then((m) => m.HistoryPageComponent)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
