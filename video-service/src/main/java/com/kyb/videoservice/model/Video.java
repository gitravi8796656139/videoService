package com.kyb.videoservice.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Video {
    private Long id;
    private String title;
    private String description;
    private String originalFileName;
    private String storedFileName;   // UUID-based name saved on disk
    private String contentType;
    private Long fileSizeBytes;
    private LocalDateTime uploadedAt;
}
