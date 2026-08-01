export interface CodeReviewRequest {
  gitDiff: string;
  prTitle?: string;
  repositoryUuid?: string;
}

export interface CodeReviewResponse {
  uuid: string;
  prTitle: string;
  qualityScore: number;
  securityIssuesCount: number;
  summary: string;
  securityAlerts: string[];
  improvements: string[];
  createdAt: string;
}
