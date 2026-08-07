export interface CodeOptimizerRequest {
  code: string;
  language?: string;
  repositoryUuid?: string;
}

export interface CodeOptimizerResponse {
  uuid: string;
  detectedLanguage: string;
  detectedFramework: string;
  optimizationConfidence: string;
  optimizationLevel: string;
  algorithmBefore?: string;
  algorithmAfter?: string;
  dataStructureBefore?: string;
  dataStructureAfter?: string;
  timeComplexityBefore?: string;
  timeComplexityAfter?: string;
  spaceComplexityBefore?: string;
  spaceComplexityAfter?: string;
  theoreticalImprovement?: string;
  bottlenecks?: string[];
  rawCode: string;
  optimizedCode: string;
  whyBetter?: string;
  tradeOffs?: string;
  whenNotToUse?: string;
  correctnessNotes?: string;
  isAlreadyOptimal: boolean;
  fullReportMarkdown: string;
  createdAt: string;
}
