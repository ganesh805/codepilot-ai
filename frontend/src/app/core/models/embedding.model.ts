export interface EmbeddingResponse {
  uuid: string;
  chunkUuid: string;
  filePath: string;
  fileName: string;
  language: string;
  qdrantPointId: string;
  vectorDimension: number;
  status: string;
  createdAt: string;
}

export interface EmbeddingStats {
  repoUuid: string;
  repoName: string;
  totalChunks: number;
  indexedVectors: number;
  vectorDimension: number;
  collectionName: string;
  distanceMetric: string;
  indexingStatus: string;
}
