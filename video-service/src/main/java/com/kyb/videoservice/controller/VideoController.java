package com.kyb.videoservice.controller;

import com.kyb.videoservice.model.VideoDTO;
import com.kyb.videoservice.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /**
     * POST /api/videos/upload
     * Multipart: file (required), title (optional), description (optional)
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<VideoDTO.ApiResponse<VideoDTO.VideoResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestPart(value = "title",       required = false) String title,
            @RequestPart(value = "description", required = false) String description) {

        VideoDTO.VideoResponse data = videoService.uploadVideo(file, title, description);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VideoDTO.ApiResponse.ok("Video uploaded successfully", data));
    }

    /**
     * GET /api/videos?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<VideoDTO.ApiResponse<VideoDTO.VideoListResponse>> list(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                VideoDTO.ApiResponse.ok("Videos fetched", videoService.listVideos(page, size)));
    }

    /**
     * GET /api/videos/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<VideoDTO.ApiResponse<VideoDTO.VideoResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(
                VideoDTO.ApiResponse.ok("Video found", videoService.getVideoById(id)));
    }

    /**
     * GET /api/videos/{id}/stream
     * Supports HTTP Range headers — works with video players and browser <video> tag.
     */
    @GetMapping("/{id}/stream")
    public ResponseEntity<Resource> stream(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {

        Resource resource   = videoService.loadAsResource(id);
        String  contentType = videoService.getContentType(id);

        return ResponseEntity
                .status(rangeHeader != null ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK)
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    /**
     * GET /api/videos/{id}/download
     * Returns file as an attachment (triggers browser download).
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Resource resource        = videoService.loadAsResource(id);
        String  contentType      = videoService.getContentType(id);
        String  originalFileName = videoService.getOriginalFileName(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + originalFileName + "\"")
                .body(resource);
    }

    /**
     * DELETE /api/videos/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<VideoDTO.ApiResponse<Void>> delete(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return ResponseEntity.ok(VideoDTO.ApiResponse.ok("Video deleted", null));
    }
}
