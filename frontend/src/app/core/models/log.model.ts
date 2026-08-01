export interface LogAnalysisResponse {
  uuid: string;
  fileName: string;
  totalLines: number;
  errorCount: number;
  warnCount: number;
  infoCount: number;
  summary: string;
  flaggedErrorLines: string[];
  createdAt: string;
}
