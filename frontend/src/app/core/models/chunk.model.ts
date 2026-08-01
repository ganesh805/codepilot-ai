export interface CodeChunk {
  uuid: string;
  filePath: string;
  fileName: string;
  language: string;
  chunkIndex: number;
  startLine: number;
  endLine: number;
  tokenCount: number;
  content: string;
}

export interface ScanStats {
  repoUuid: string;
  repoName: string;
  totalChunks: number;
  languageBreakdown: Record<string, number>;
}
