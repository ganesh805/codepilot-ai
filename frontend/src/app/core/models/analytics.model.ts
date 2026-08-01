export interface AnalyticsMetrics {
  totalUsers: number;
  totalRepositories: number;
  totalCodeChunks: number;
  totalEmbeddings: number;
  totalAiChats: number;
  totalExceptionAnalyses: number;
  totalLogAnalyses: number;
  totalCodeReviews: number;
  totalApiDocs: number;
  totalSqlOptimizations: number;
  systemStatus: string;
  javaVersion: string;
  springVersion: string;
  serverTime: string;
}
