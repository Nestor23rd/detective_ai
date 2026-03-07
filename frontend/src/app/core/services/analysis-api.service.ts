import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import {
  AnalysisResult,
  AnalyzePayload,
  FollowUpResponse,
  HistoryItem,
  UploadAnalyzePayload
} from '../models/analysis.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AnalysisApiService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = environment.apiBaseUrl;

  analyze(payload: AnalyzePayload) {
    return this.http.post<AnalysisResult>(`${this.apiBaseUrl}/analyze`, payload);
  }

  analyzeUpload(payload: UploadAnalyzePayload) {
    const formData = new FormData();
    formData.append('file', payload.file);

    const params = new HttpParams().set('ocrEnabled', String(payload.ocrEnabled ?? true));
    return this.http.post<AnalysisResult>(`${this.apiBaseUrl}/analyze/upload`, formData, { params });
  }

  getHistory() {
    return this.http.get<HistoryItem[]>(`${this.apiBaseUrl}/history`);
  }

  getAnalysis(id: string) {
    return this.http.get<AnalysisResult>(`${this.apiBaseUrl}/history/${id}`);
  }

  askFollowUp(id: string, question: string) {
    return this.http.post<FollowUpResponse>(`${this.apiBaseUrl}/history/${id}/follow-up`, { question });
  }
}
