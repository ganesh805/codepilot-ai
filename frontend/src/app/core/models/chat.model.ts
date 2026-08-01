export interface CodeCitation {
  filePath: string;
  fileName: string;
  language: string;
  startLine: number;
  endLine: number;
  similarityScore: number;
  content: string;
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  answer: string;
  citations: CodeCitation[];
}

export interface ChatMessage {
  id: string;
  sender: 'USER' | 'ASSISTANT';
  text: string;
  citations?: CodeCitation[];
  timestamp: Date;
}
