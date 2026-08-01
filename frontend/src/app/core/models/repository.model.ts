export interface CodeRepository {
  uuid: string;
  name: string;
  owner: string;
  gitUrl?: string;
  importType: 'GITHUB' | 'ZIP_UPLOAD';
  defaultBranch: string;
  fileCount: number;
  totalSizeBytes: number;
  status: 'CLONING' | 'EXTRACTING' | 'READY' | 'FAILED';
  createdAt: string;
}

export interface GitImportRequest {
  gitUrl: string;
  branch?: string;
}
