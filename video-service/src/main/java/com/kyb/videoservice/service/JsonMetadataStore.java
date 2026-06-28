package com.kyb.videoservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kyb.videoservice.model.Video;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Stores all video metadata in a single JSON file on disk.
 * Thread-safe via ReadWriteLock.
 * No database required.
 */
@Slf4j
@Component
public class JsonMetadataStore {

    @Value("${video.storage.path}")
    private String storagePath;

    private Path metadataFile;
    private final ObjectMapper mapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // In-memory map: id -> Video
    private Map<Long, Video> store = new LinkedHashMap<>();
    private final AtomicLong idCounter = new AtomicLong(1);

    public JsonMetadataStore() {
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
    }

    @PostConstruct
    public void init() throws IOException {
        Path dir = Paths.get(storagePath);
        Files.createDirectories(dir);
        metadataFile = dir.resolve("metadata.json");

        if (Files.exists(metadataFile)) {
            load();
            // Set id counter beyond the highest existing id
            store.keySet().stream().max(Long::compareTo)
                    .ifPresent(max -> idCounter.set(max + 1));
            log.info("Loaded {} video records from metadata.json", store.size());
        } else {
            save();
            log.info("Created new metadata.json at {}", metadataFile);
        }
    }

    // ---- CRUD ----

    public Video save(Video video) {
        lock.writeLock().lock();
        try {
            if (video.getId() == null) {
                video.setId(idCounter.getAndIncrement());
            }
            store.put(video.getId(), video);
            persist();
            return video;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Optional<Video> findById(Long id) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(store.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    public List<Video> findAll() {
        lock.readLock().lock();
        try {
            // Return sorted newest first
            List<Video> list = new ArrayList<>(store.values());
            list.sort((a, b) -> b.getUploadedAt().compareTo(a.getUploadedAt()));
            return list;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void delete(Long id) {
        lock.writeLock().lock();
        try {
            store.remove(id);
            persist();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public long count() {
        lock.readLock().lock();
        try { return store.size(); }
        finally { lock.readLock().unlock(); }
    }

    // ---- Internal ----

    private void load() throws IOException {
        List<Video> list = mapper.readValue(metadataFile.toFile(),
                new TypeReference<List<Video>>() {});
        store = new LinkedHashMap<>();
        for (Video v : list) store.put(v.getId(), v);
    }

    private void persist() {
        try {
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(metadataFile.toFile(), new ArrayList<>(store.values()));
        } catch (IOException e) {
            log.error("Failed to persist metadata.json", e);
            throw new RuntimeException("Could not save metadata: " + e.getMessage(), e);
        }
    }

    private void save() {
        persist();
    }
}
