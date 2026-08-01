export interface ExceptionAnalysisRequest {
  stackTrace: string;
  repositoryUuid?: string;
}

export interface ExceptionAnalysisResponse {
  uuid: string;
  exceptionType: string;
  errorMessage: string;
  rootCause: string;
  suggestedFix: string;
  createdAt: string;
}
