package com.codepilot.service;

import com.codepilot.dto.*;
import com.codepilot.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AiChatRagService {

    private static final Logger log = LoggerFactory.getLogger(AiChatRagService.class);

    private final SemanticSearchService searchService;
    private final AiModelRouterService modelRouterService;

    public AiChatRagService(SemanticSearchService searchService, AiModelRouterService modelRouterService) {
        this.searchService = searchService;
        this.modelRouterService = modelRouterService;
    }

    public ChatResponse processChatRequest(User user, String repoUuid, ChatRequest request) {
        log.info("Processing AI Chat RAG query for user {}: '{}' using AI Provider: {}", 
                user.getUsername(), request.getMessage(), request.getAiProvider());

        // Step 1: Retrieve top-3 relevant code context snippets via Semantic Search
        SearchRequest searchReq = SearchRequest.builder()
                .query(request.getMessage())
                .topK(3)
                .build();

        List<SearchResultDTO> searchResults = searchService.searchCodebase(user, repoUuid, searchReq);

        List<CodeCitation> citations = new ArrayList<>();
        StringBuilder contextBuilder = new StringBuilder();

        for (SearchResultDTO res : searchResults) {
            CodeCitation citation = CodeCitation.builder()
                    .filePath(res.getFilePath())
                    .fileName(res.getFileName())
                    .language(res.getLanguage())
                    .startLine(res.getStartLine())
                    .endLine(res.getEndLine())
                    .similarityScore(res.getSimilarityScore())
                    .content(res.getContent())
                    .build();
            citations.add(citation);

            contextBuilder.append(String.format("File: %s (Lines %d-%d)\nLanguage: %s\n```\n%s\n```\n\n",
                    res.getFilePath(), res.getStartLine(), res.getEndLine(), res.getLanguage(), res.getContent()));
        }

        if (citations.isEmpty()) {
            return ChatResponse.builder()
                    .answer("I couldn't find any relevant code snippets in the repository to answer your question. Please ensure the codebase has been scanned and vector indexed.")
                    .citations(new ArrayList<>())
                    .build();
        }

        // Step 2: Route prompt to selected AI Model Provider (Gemini, OpenAI GPT-4o, DeepSeek, or Hybrid Ensemble)
        String systemPrompt = "You are CodePilot AI, an expert Senior Software Architect pair-programming assistant.";
        String synthesizedAnswer = modelRouterService.generateResponse(
                request.getAiProvider(), 
                systemPrompt, 
                request.getMessage(), 
                contextBuilder.toString()
        );

        return ChatResponse.builder()
                .answer(synthesizedAnswer)
                .citations(citations)
                .build();
    }
}
