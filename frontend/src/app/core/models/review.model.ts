export interface CodeReviewRequest {
  gitDiff: string;
  prTitle?: string;
  repositoryUuid?: string;
}

export interface CodeReviewResponse {
  uuid: string;
  prTitle: string;
  qualityScore: number;
  securityScore?: number;
  codeQualityScore?: number;
  maintainabilityScore?: number;
  performanceScore?: number;
  bestPracticeScore?: number;
  securityIssuesCount: number;
  mergeRecommendation?: string;
  reviewDurationMs?: number;

  criticalCount?: number;
  highCount?: number;
  mediumCount?: number;
  lowCount?: number;

  summary: string;
  securityAlerts: string[];
  improvements: string[];
  performanceAlerts?: string[];
  maintainabilityAlerts?: string[];
  bestPracticeAlerts?: string[];
  positiveObservations?: string[];
  prioritizedRecommendations?: string[];

  createdAt: string;
}
