package com.codepilot.service;

import com.codepilot.dto.CodeRepositoryResponse;
import com.codepilot.dto.GitImportRequest;
import com.codepilot.entity.CodeRepository;
import com.codepilot.entity.ImportType;
import com.codepilot.entity.RepositoryStatus;
import com.codepilot.entity.User;
import com.codepilot.repository.CodeRepositoryRepository;
import org.eclipse.jgit.api.Git;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class RepositoryImportService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryImportService.class);

    private final CodeRepositoryRepository repoRepository;
    private final String baseStoragePath;

    public RepositoryImportService(
            CodeRepositoryRepository repoRepository,
            @Value("${app.storage.base-path:C:/Users/navya/codepilot-ai/storage/repos}") String baseStoragePath) {
        this.repoRepository = repoRepository;
        this.baseStoragePath = baseStoragePath;
    }

    @Transactional
    public CodeRepositoryResponse importFromGithub(User user, GitImportRequest request) {
        String repoUuid = UUID.randomUUID().toString();
        String parsedName = extractRepoName(request.getGitUrl());
        String parsedOwner = extractRepoOwner(request.getGitUrl());

        Path targetDir = Paths.get(baseStoragePath, user.getId().toString(), repoUuid);

        CodeRepository repository = CodeRepository.builder()
                .uuid(repoUuid)
                .user(user)
                .name(parsedName)
                .owner(parsedOwner)
                .gitUrl(request.getGitUrl())
                .importType(ImportType.GITHUB)
                .storagePath(targetDir.toAbsolutePath().toString())
                .defaultBranch(request.getBranch() != null ? request.getBranch() : "main")
                .status(RepositoryStatus.CLONING)
                .build();

        CodeRepository savedRepo = repoRepository.save(repository);

        try {
            log.info("Cloning GitHub repository {} to {}", request.getGitUrl(), targetDir);
            Files.createDirectories(targetDir);

            Git.cloneRepository()
                    .setURI(request.getGitUrl())
                    .setDirectory(targetDir.toFile())
                    .setBranch(request.getBranch())
                    .call();

            long[] stats = calculateDirectoryStats(targetDir);
            savedRepo.setFileCount((int) stats[0]);
            savedRepo.setTotalSizeBytes(stats[1]);
            savedRepo.setStatus(RepositoryStatus.READY);

        } catch (Exception ex) {
            log.error("Failed to clone GitHub repository", ex);
            savedRepo.setStatus(RepositoryStatus.FAILED);
            throw new RuntimeException("Failed to clone GitHub repository: " + ex.getMessage(), ex);
        }

        return mapToResponse(repoRepository.save(savedRepo));
    }

    @Transactional
    public CodeRepositoryResponse importFromZip(User user, MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded ZIP file cannot be empty");
        }

        String repoUuid = UUID.randomUUID().toString();
        String originalFilename = file.getOriginalFilename();
        String repoName = originalFilename != null && originalFilename.endsWith(".zip")
                ? originalFilename.substring(0, originalFilename.length() - 4)
                : "unnamed-zip-repo";

        Path targetDir = Paths.get(baseStoragePath, user.getId().toString(), repoUuid);

        CodeRepository repository = CodeRepository.builder()
                .uuid(repoUuid)
                .user(user)
                .name(repoName)
                .owner(user.getUsername())
                .importType(ImportType.ZIP_UPLOAD)
                .storagePath(targetDir.toAbsolutePath().toString())
                .status(RepositoryStatus.EXTRACTING)
                .build();

        CodeRepository savedRepo = repoRepository.save(repository);

        try {
            Files.createDirectories(targetDir);
            unzipSafely(file.getInputStream(), targetDir);

            long[] stats = calculateDirectoryStats(targetDir);
            savedRepo.setFileCount((int) stats[0]);
            savedRepo.setTotalSizeBytes(stats[1]);
            savedRepo.setStatus(RepositoryStatus.READY);

        } catch (Exception ex) {
            log.error("Failed to extract ZIP repository", ex);
            savedRepo.setStatus(RepositoryStatus.FAILED);
            throw new RuntimeException("Failed to extract ZIP upload: " + ex.getMessage(), ex);
        }

        return mapToResponse(repoRepository.save(savedRepo));
    }

    @Transactional(readOnly = true)
    public List<CodeRepositoryResponse> getUserRepositories(User user) {
        return repoRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CodeRepositoryResponse getRepositoryByUuid(User user, String uuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(uuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found with UUID: " + uuid));
        return mapToResponse(repo);
    }

    @Transactional
    public void deleteRepository(User user, String uuid) {
        CodeRepository repo = repoRepository.findByUuidAndUserId(uuid, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found"));

        Path storageDir = Paths.get(repo.getStoragePath());
        try {
            FileSystemUtils.deleteRecursively(storageDir);
        } catch (IOException ex) {
            log.warn("Could not delete local storage directory: {}", storageDir, ex);
        }

        repoRepository.delete(repo);
    }

    private void unzipSafely(java.io.InputStream inputStream, Path targetDir) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry zipEntry = zis.getNextEntry();
            byte[] buffer = new byte[8192];

            while (zipEntry != null) {
                Path newPath = zipSlipProtect(zipEntry, targetDir);

                if (zipEntry.isDirectory()) {
                    Files.createDirectories(newPath);
                } else {
                    if (newPath.getParent() != null) {
                        Files.createDirectories(newPath.getParent());
                    }
                    try (FileOutputStream fos = new FileOutputStream(newPath.toFile())) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }

    private Path zipSlipProtect(ZipEntry zipEntry, Path targetDir) throws IOException {
        Path targetFile = targetDir.resolve(zipEntry.getName()).normalize();
        if (!targetFile.startsWith(targetDir.normalize())) {
            throw new SecurityException("Bad zip entry (Zip Slip vulnerability attempt): " + zipEntry.getName());
        }
        return targetFile;
    }

    private long[] calculateDirectoryStats(Path targetDir) throws IOException {
        long fileCount = 0;
        long totalBytes = 0;

        try (Stream<Path> stream = Files.walk(targetDir)) {
            List<Path> paths = stream.filter(p -> !p.toString().contains(File.separator + ".git" + File.separator))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());

            fileCount = paths.size();
            for (Path path : paths) {
                totalBytes += Files.size(path);
            }
        }
        return new long[]{fileCount, totalBytes};
    }

    private String extractRepoName(String gitUrl) {
        if (gitUrl == null) return "unknown-repo";
        String cleanUrl = gitUrl.endsWith(".git") ? gitUrl.substring(0, gitUrl.length() - 4) : gitUrl;
        int lastSlash = cleanUrl.lastIndexOf('/');
        return lastSlash >= 0 ? cleanUrl.substring(lastSlash + 1) : cleanUrl;
    }

    private String extractRepoOwner(String gitUrl) {
        if (gitUrl == null) return "unknown";
        String cleanUrl = gitUrl.endsWith(".git") ? gitUrl.substring(0, gitUrl.length() - 4) : gitUrl;
        String[] parts = cleanUrl.split("/");
        return parts.length >= 2 ? parts[parts.length - 2] : "unknown";
    }

    private CodeRepositoryResponse mapToResponse(CodeRepository repo) {
        return CodeRepositoryResponse.builder()
                .uuid(repo.getUuid())
                .name(repo.getName())
                .owner(repo.getOwner())
                .gitUrl(repo.getGitUrl())
                .importType(repo.getImportType())
                .defaultBranch(repo.getDefaultBranch())
                .fileCount(repo.getFileCount())
                .totalSizeBytes(repo.getTotalSizeBytes())
                .status(repo.getStatus())
                .createdAt(repo.getCreatedAt())
                .build();
    }
}
