package com.kyb.videoservice.config;

import com.kyb.videoservice.model.VideoDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<VideoDTO.ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(VideoDTO.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<VideoDTO.ApiResponse<Void>> handleRuntime(RuntimeException ex) {
        log.error("Error: {}", ex.getMessage());
        boolean notFound = ex.getMessage() != null && ex.getMessage().contains("not found");
        HttpStatus status = notFound ? HttpStatus.NOT_FOUND : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(VideoDTO.ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<VideoDTO.ApiResponse<Void>> handleMaxSize() {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(VideoDTO.ApiResponse.error("File size exceeds the maximum allowed limit (5 GB)"));
    }
}
