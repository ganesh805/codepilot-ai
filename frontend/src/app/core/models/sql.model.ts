export interface SqlQueryRequest {
  rawSql: string;
  repositoryUuid?: string;
}

export interface SqlQueryResponse {
  uuid: string;
  rawSql: string;
  optimizedSql: string;
  indexingDdl: string;
  performanceGainPct: number;
  analysisSummary: string;
  detectedAntiPatterns: string[];
  createdAt: string;
}
