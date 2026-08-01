export interface EndpointSummary {
  httpMethod: string;
  path: string;
  controllerClass: string;
  methodName: string;
}

export interface ApiDocResponse {
  uuid: string;
  repoName: string;
  totalEndpoints: number;
  markdownSpec: string;
  openapiJson: string;
  endpoints: EndpointSummary[];
  createdAt: string;
}
