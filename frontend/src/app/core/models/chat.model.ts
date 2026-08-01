export type AiProvider = 'GEMINI' | 'OPENAI' | 'DEEPSEEK' | 'HYBRID_ENSEMBLE';

export interface CodeCitation {
  filePath: string;
  fileName: string;
  language: string;
  startLine: number;
  endLine: number;
  similarityScore: number;
  content: string;
}

export interface ChatMessage {
  id: string;
  sender: 'USER' | 'ASSISTANT';
  text: string;
  aiProvider?: AiProvider;
  citations?: CodeCitation[];
  timestamp: Date;
}

export interface ChatRequest {
  message: string;
  repositoryUuid?: string;
  aiProvider?: AiProvider;
}

export interface ChatResponse {
  answer: string;
  citations: CodeCitation[];
}
