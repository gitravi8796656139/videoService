package com.kyb.videoservice.service;

import com.kyb.videoservice.model.Video;
import com.kyb.videoservice.model.VideoDTO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final JsonMetadataStore metadataStore;

    @Value("${video.storage.path}")
    private String storagePath;

    @Value("${video.allowed-types}")
    private String allowedTypesRaw;

    private Path storageDir;
    private List<String> allowedTypes;

    @PostConstruct
    public void init() throws IOException {
        storageDir = Paths.get(storagePath);
        Files.createDirectories(storageDir);
        allowedTypes = Arrays.asList(allowedTypesRaw.split(","));
    }

    // ---------------------------------------------------------------- upload

    public VideoDTO.VideoResponse uploadVideo(MultipartFile file, String title, String description) {
        validateFile(file);

        String originalName = file.getOriginalFilename();
        String extension    = getExtension(originalName);
        String storedName   = UUID.randomUUID() + "." + extension;
        Path   target       = storageDir.resolve(storedName);

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save file to disk: " + e.getMessage(), e);
        }

        Video video = Video.builder()
                .title(title != null && !title.isBlank() ? title : originalName)
                .description(description)
                .originalFileName(originalName)
                .storedFileName(storedName)
                .contentType(file.getContentType())
                .fileSizeBytes(file.getSize())
                .uploadedAt(LocalDateTime.now())
                .build();

        video = metadataStore.save(video);
        log.info("Uploaded video id={} file={}", video.getId(), storedName);
        return toResponse(video);
    }

    // ---------------------------------------------------------------- list (paginated)

    public VideoDTO.VideoListResponse listVideos(int page, int size) {
        List<Video> all = metadataStore.findAll();
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);

        int fromIndex = page * size;
        int toIndex   = Math.min(fromIndex + size, total);

        List<Video> pageContent = fromIndex >= total
                ? Collections.emptyList()
                : all.subList(fromIndex, toIndex);

        return VideoDTO.VideoListResponse.builder()
                .videos(pageContent.stream().map(this::toResponse).toList())
                .page(page)
                .size(size)
                .totalElements(total)
                .totalPages(totalPages)
                .build();
    }

    // ---------------------------------------------------------------- single

    public VideoDTO.VideoResponse getVideoById(Long id) {
        return toResponse(requireVideo(id));
    }

    // ---------------------------------------------------------------- stream / download

    public Resource loadAsResource(Long id) {
        Video video = requireVideo(id);
        Path filePath = storageDir.resolve(video.getStoredFileName());
        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) return resource;
            throw new RuntimeException("File is not readable: " + video.getStoredFileName());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not resolve file path", e);
        }
    }

    public String getContentType(Long id) {
        return requireVideo(id).getContentType();
    }

    public String getOriginalFileName(Long id) {
        return requireVideo(id).getOriginalFileName();
    }

    // ---------------------------------------------------------------- delete

    public void deleteVideo(Long id) {
        Video video = requireVideo(id);
        try {
            Files.deleteIfExists(storageDir.resolve(video.getStoredFileName()));
        } catch (IOException e) {
            log.warn("Could not delete file on disk: {}", video.getStoredFileName());
        }
        metadataStore.delete(id);
        log.info("Deleted video id={}", id);
    }

    // ---------------------------------------------------------------- helpers

    private Video requireVideo(Long id) {
        return metadataStore.findById(id)
                .orElseThrow(() -> new RuntimeException("Video not found with id: " + id));
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("File is empty or missing");
        String ct = file.getContentType();
        if (ct == null || !allowedTypes.contains(ct))
            throw new IllegalArgumentException(
                    "Unsupported type: " + ct + ". Allowed: " + allowedTypesRaw);
    }

    private String getExtension(String name) {
        if (name == null || !name.contains(".")) return "mp4";
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase();
    }

    private VideoDTO.VideoResponse toResponse(Video v) {
        double mb = v.getFileSizeBytes() / (1024.0 * 1024.0);
        return VideoDTO.VideoResponse.builder()
                .id(v.getId())
                .title(v.getTitle())
                .description(v.getDescription())
                .originalFileName(v.getOriginalFileName())
                .contentType(v.getContentType())
                .fileSizeBytes(v.getFileSizeBytes())
                .fileSizeMb(String.format("%.2f MB", mb))
                .uploadedAt(v.getUploadedAt())
                .downloadUrl("/api/videos/" + v.getId() + "/download")
                .streamUrl("/api/videos/" + v.getId() + "/stream")
                .build();
    }
}
