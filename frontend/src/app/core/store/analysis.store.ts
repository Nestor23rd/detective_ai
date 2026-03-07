import { Injectable, signal } from '@angular/core';
import { AnalysisResult, HistoryItem } from '../models/analysis.model';

@Injectable({ providedIn: 'root' })
export class AnalysisStore {
  readonly latestAnalysis = signal<AnalysisResult | null>(null);
  readonly history = signal<HistoryItem[]>([]);
  readonly isAnalyzing = signal(false);
  readonly activeModel = signal('asi1');

  setLatestAnalysis(result: AnalysisResult | null) {
    this.latestAnalysis.set(result);
  }

  setHistory(items: HistoryItem[]) {
    this.history.set(items);
  }

  startAnalysis(model = 'asi1') {
    this.activeModel.set(model);
    this.isAnalyzing.set(true);
  }

  finishAnalysis() {
    this.isAnalyzing.set(false);
  }
}
