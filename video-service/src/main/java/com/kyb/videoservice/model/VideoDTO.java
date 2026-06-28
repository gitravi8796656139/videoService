package com.kyb.videoservice.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

public class VideoDTO {

    @Data
    @Builder
    public static class VideoResponse {
        private Long id;
        private String title;
        private String description;
        private String originalFileName;
        private String contentType;
        private Long fileSizeBytes;
        private String fileSizeMb;
        private LocalDateTime uploadedAt;
        private String downloadUrl;
        private String streamUrl;
    }

    @Data
    @Builder
    public static class VideoListResponse {
        private List<VideoResponse> videos;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
    }

    @Data
    @Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> ApiResponse<T> ok(String message, T data) {
            return ApiResponse.<T>builder().success(true).message(message).data(data).build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder().success(false).message(message).build();
        }
    }
}
