package com.codepilot.service;

import com.codepilot.dto.CodeChunkResponse;
import com.codepilot.entity.CodeChunk;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeChunkRepository;
import com.codepilot.repository.CodeRepositoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class RepositoryScannerService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryScannerService.class);

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", "node_modules", "target", "dist", "build", ".mvn", ".idea", ".vscode", "coverage", ".angular"
    );

    private static final Set<String> BINARY_EXTENSIONS = Set.of(
            "png", "jpg", "jpeg", "gif", "ico", "svg", "pdf", "zip", "tar", "gz", "exe", "dll", "jar", "class", "so", "dylib"
    );

    private final CodeRepositoryRepository repoRepository;
    private final CodeChunkRepository chunkRepository;
    private final CodeChunkerEngine chunkerEngine;

    public RepositoryScannerService(
            CodeRepositoryRepository repoRepository,
            CodeChunkRepository chunkRepository,
            CodeChunkerEngine chunkerEngine) {
        this.repoRepository = repoRepository;
        this.chunkRepository = chunkRepository;
        this.chunkerEngine = chunkerEngine;
    }

    @Transactional
    public List<CodeChunkResponse> scanAndChunkRepository(User user, String repoUuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with UUID: " + repoUuid));

        Path rootPath = Paths.get(repo.getStoragePath());
        if (!Files.exists(rootPath)) {
            throw new IllegalStateException("Repository storage directory does not exist: " + repo.getStoragePath());
        }

        // Clean prior chunks if re-scanning
        chunkRepository.deleteByRepositoryId(repo.getId());

        List<CodeChunk> newChunks = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(rootPath)) {
            List<Path> validFiles = stream.filter(p -> shouldProcessFile(rootPath, p))
                    .collect(Collectors.toList());

            for (Path filePath : validFiles) {
                try {
                    List<String> lines = Files.readAllLines(filePath);
                    String relativePath = rootPath.relativize(filePath).toString().replace('\\', '/');
                    String fileName = filePath.getFileName().toString();

                    List<CodeChunk> fileChunks = chunkerEngine.chunkFile(repo, relativePath, fileName, lines);
                    newChunks.addAll(fileChunks);

                } catch (IOException ex) {
                    log.warn("Skipping file {} due to read error: {}", filePath, ex.getMessage());
                }
            }

            List<CodeChunk> savedChunks = chunkRepository.saveAll(newChunks);
            log.info("Successfully scanned repository {} and created {} chunks", repo.getName(), savedChunks.size());

            return savedChunks.stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());

        } catch (IOException ex) {
            log.error("Failed during repository tree scanning", ex);
            throw new RuntimeException("Error scanning repository tree: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<CodeChunkResponse> getRepositoryChunks(User user, String repoUuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with UUID: " + repoUuid));

        return chunkRepository.findByRepositoryIdOrderByFilePathAscChunkIndexAsc(repo.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRepositoryScanStats(User user, String repoUuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(repoUuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        long totalChunks = chunkRepository.countByRepositoryId(repo.getId());
        List<Object[]> rawStats = chunkRepository.getLanguageStatsByRepositoryId(repo.getId());

        Map<String, Long> languageStats = new HashMap<>();
        for (Object[] row : rawStats) {
            languageStats.put((String) row[0], (Long) row[1]);
        }

        return Map.of(
                "repoUuid", repoUuid,
                "repoName", repo.getName(),
                "totalChunks", totalChunks,
                "languageBreakdown", languageStats
        );
    }

    private boolean shouldProcessFile(Path rootPath, Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        Path relativePath = rootPath.relativize(path);
        for (Path part : relativePath) {
            if (IGNORED_DIRECTORIES.contains(part.toString())) {
                return false;
            }
        }

        String fileName = path.getFileName().toString();
        if (fileName.contains(".")) {
            String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            if (BINARY_EXTENSIONS.contains(ext)) {
                return false;
            }
        }

        return true;
    }

    private CodeChunkResponse mapToResponse(CodeChunk chunk) {
        return CodeChunkResponse.builder()
                .uuid(chunk.getUuid())
                .filePath(chunk.getFilePath())
                .fileName(chunk.getFileName())
                .language(chunk.getLanguage())
                .chunkIndex(chunk.getChunkIndex())
                .startLine(chunk.getStartLine())
                .endLine(chunk.getEndLine())
                .tokenCount(chunk.getTokenCount())
                .content(chunk.getContent())
                .build();
    }
}
