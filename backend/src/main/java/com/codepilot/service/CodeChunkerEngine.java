package com.codepilot.service;

import com.codepilot.entity.CodeChunk;
import com.codepilot.entity.CodeRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class CodeChunkerEngine {

    private static final int DEFAULT_CHUNK_LINES = 50;
    private static final int DEFAULT_OVERLAP_LINES = 10;

    private static final Map<String, String> EXTENSION_LANGUAGE_MAP = Map.ofEntries(
            Map.entry("java", "Java"),
            Map.entry("ts", "TypeScript"),
            Map.entry("js", "JavaScript"),
            Map.entry("py", "Python"),
            Map.entry("cpp", "C++"),
            Map.entry("c", "C"),
            Map.entry("h", "C/C++ Header"),
            Map.entry("go", "Go"),
            Map.entry("rs", "Rust"),
            Map.entry("sql", "SQL"),
            Map.entry("html", "HTML"),
            Map.entry("css", "CSS"),
            Map.entry("scss", "SCSS"),
            Map.entry("json", "JSON"),
            Map.entry("yml", "YAML"),
            Map.entry("yaml", "YAML"),
            Map.entry("md", "Markdown"),
            Map.entry("xml", "XML")
    );

    public String detectLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "PlainText";
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        return EXTENSION_LANGUAGE_MAP.getOrDefault(ext, "PlainText");
    }

    public List<CodeChunk> chunkFile(CodeRepository repository, String relativePath, String fileName, List<String> fileLines) {
        List<CodeChunk> chunks = new ArrayList<>();
        if (fileLines == null || fileLines.isEmpty()) {
            return chunks;
        }

        String language = detectLanguage(fileName);
        int totalLines = fileLines.size();

        int startLine = 1;
        int chunkIndex = 0;

        while (startLine <= totalLines) {
            int endLine = Math.min(startLine + DEFAULT_CHUNK_LINES - 1, totalLines);

            List<String> chunkLines = fileLines.subList(startLine - 1, endLine);
            String chunkContent = String.join("\n", chunkLines);

            if (!chunkContent.trim().isEmpty()) {
                int tokenCount = estimateTokenCount(chunkContent);

                CodeChunk chunk = CodeChunk.builder()
                        .repository(repository)
                        .filePath(relativePath)
                        .fileName(fileName)
                        .language(language)
                        .chunkIndex(chunkIndex++)
                        .startLine(startLine)
                        .endLine(endLine)
                        .tokenCount(tokenCount)
                        .content(chunkContent)
                        .build();

                chunks.add(chunk);
            }

            if (endLine == totalLines) {
                break;
            }

            startLine += (DEFAULT_CHUNK_LINES - DEFAULT_OVERLAP_LINES);
        }

        return chunks;
    }

    public int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        // Heuristic: ~4 characters per token or whitespace split
        return (int) Math.ceil(content.length() / 4.0);
    }
}
