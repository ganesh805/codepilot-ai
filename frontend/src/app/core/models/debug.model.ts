export interface ExceptionAnalysisRequest {
  stackTrace: string;
  repositoryUuid?: string;
}

export interface ExceptionAnalysisResponse {
  uuid: string;
  exceptionType: string;
  errorMessage: string;
  severity?: string;
  confidenceScore?: number;
  confidenceReason?: string;
  rootCauseSummary?: string;
  estimatedFixTime?: string;
  productionImpact?: string;
  mergeRisk?: string;

  rootCauseFile?: string;
  rootCauseClass?: string;
  rootCauseMethod?: string;
  rootCauseLineNumber?: number;

  evidenceList?: string[];
  possibleCausesMap?: { [key: string]: number };
  businessImpact?: string;
  recommendedFix?: string;
  fixedCodeExample?: string;
  debugChecklist?: string[];
  relatedTechnologies?: string[];
  preventiveRecommendations?: string[];
  learningResources?: string[];
  timelineSteps?: string[];
  analysisDurationMs?: number;

  fullReportMarkdown?: string;
  rootCause: string;
  suggestedFix: string;
  createdAt: string;
}
