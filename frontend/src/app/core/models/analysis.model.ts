export type InputType = 'TEXT' | 'URL' | 'IMAGE' | 'PDF' | 'VIDEO';
export type Verdict = 'Likely Fake' | 'Questionable' | 'Likely True' | 'Verified';
export type ClaimAssessment = 'Supported' | 'Questionable' | 'Contradicted' | 'Insufficient Evidence';
export type AnalysisStatus = 'COMPLETED' | 'LIMITED' | 'CANNOT_ANALYZE';

export interface ClaimInsight {
  statement: string;
  suspicion: string;
  evidenceHint: string;
  assessment: ClaimAssessment | string;
  confidence: number;
  evidencePoints: string[];
  sourceUrls: string[];
  nextStep: string;
}

export interface EntityInsight {
  name: string;
  type: string;
  context: string;
}

export interface AnalysisResult {
  id: string;
  inputType: InputType;
  sourceUrl: string | null;
  sourceTitle: string | null;
  sourceLabel: string | null;
  imageMimeType: string | null;
  modelUsed: string;
  analysisStatus: AnalysisStatus;
  statusReason: string | null;
  contentLanguage: string;
  riskLabels: string[];
  visitedUrls: string[];
  summary: string;
  claims: ClaimInsight[];
  entities: EntityInsight[];
  credibilityScore: number | null;
  verdict: Verdict | null;
  reasoning: string;
  limitations: string[];
  recommendedChecks: string[];
  createdAt: string;
}

export interface HistoryItem {
  id: string;
  inputType: InputType;
  sourceUrl: string | null;
  sourceTitle: string | null;
  sourceLabel: string | null;
  modelUsed: string | null;
  analysisStatus: AnalysisStatus;
  statusReason: string | null;
  contentLanguage: string | null;
  riskLabels: string[];
  summary: string;
  credibilityScore: number | null;
  verdict: Verdict | null;
  createdAt: string;
}

export interface ImageAnalyzePayload {
  imageBase64: string;
  imageMimeType: string;
  imageName?: string;
}

export interface AnalyzePayload {
  text?: string;
  url?: string;
  imageBase64?: string;
  imageMimeType?: string;
  imageName?: string;
  videoUrl?: string;
}

export interface UploadAnalyzePayload {
  file: File;
  ocrEnabled?: boolean;
}

export interface FollowUpResponse {
  answer: string;
  sourceUrls: string[];
  suggestedChecks: string[];
}

export function verdictTone(verdict: Verdict | null): string {
  if (!verdict) {
    return 'bg-slate-100 text-slate-700 border-slate-200';
  }
  switch (verdict) {
    case 'Likely Fake':
      return 'bg-rose-100 text-rose-700 border-rose-200';
    case 'Questionable':
      return 'bg-amber-100 text-amber-700 border-amber-200';
    case 'Likely True':
      return 'bg-sky-100 text-sky-700 border-sky-200';
    case 'Verified':
      return 'bg-emerald-100 text-emerald-700 border-emerald-200';
  }
}

export function claimAssessmentTone(assessment: string): string {
  switch (assessment) {
    case 'Supported':
      return 'bg-emerald-100 text-emerald-700 border-emerald-200';
    case 'Contradicted':
      return 'bg-rose-100 text-rose-700 border-rose-200';
    case 'Insufficient Evidence':
      return 'bg-slate-100 text-slate-700 border-slate-200';
    default:
      return 'bg-amber-100 text-amber-700 border-amber-200';
  }
}

export function scoreTone(score: number | null): string {
  if (score === null || score === undefined) {
    return 'from-slate-400 to-slate-300';
  }
  if (score < 30) {
    return 'from-rose-500 to-orange-400';
  }
  if (score < 60) {
    return 'from-amber-500 to-orange-300';
  }
  if (score < 80) {
    return 'from-sky-500 to-teal-400';
  }
  return 'from-emerald-500 to-teal-400';
}

export function scoreLabel(score: number | null): string {
  if (score === null || score === undefined) {
    return 'Analysis unavailable';
  }
  if (score < 30) {
    return 'Very likely fake';
  }
  if (score < 60) {
    return 'Suspicious or questionable';
  }
  if (score < 80) {
    return 'Likely credible';
  }
  return 'Highly credible or verified';
}

export function analysisStatusLabel(status: AnalysisStatus): string {
  switch (status) {
    case 'CANNOT_ANALYZE':
      return 'Cannot analyze';
    case 'LIMITED':
      return 'Limited analysis';
    default:
      return 'Analysis complete';
  }
}

export function inputTypeLabel(inputType: InputType): string {
  switch (inputType) {
    case 'URL':
      return 'URL investigation';
    case 'IMAGE':
      return 'Image investigation';
    case 'PDF':
      return 'Document investigation';
    case 'VIDEO':
      return 'Video investigation';
    default:
      return 'Text investigation';
  }
}
