export interface SearchRequest {
  query: string;
  topK?: number;
}

export interface SearchResult {
  chunkUuid: string;
  filePath: string;
  fileName: string;
  language: string;
  startLine: number;
  endLine: number;
  tokenCount: number;
  similarityScore: number;
  content: string;
}
