package com.lockedfunds.video.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lockedfunds.video.dto.DataResponseDTO;
import com.lockedfunds.video.dto.VideoDTO;
import com.lockedfunds.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
public class VideoController {
    private final VideoService videoService;
    @PostMapping("/upload")
    public CompletableFuture<ResponseEntity<DataResponseDTO>> uploadVideo(@RequestParam("data") String videoDTOString,
                                                                          @RequestPart("file") MultipartFile file) throws JsonProcessingException {

        ObjectMapper mapper = new ObjectMapper();
        VideoDTO videoDTO = mapper.readValue(videoDTOString, VideoDTO.class);

        return videoService.uploadVideo(videoDTO, file)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> ResponseEntity.status(500)
                        .body(DataResponseDTO.builder()
                                .status("500")
                                .message("Upload failed: " + ex.getMessage())
                                .build()));
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<StreamingResponseBody> streamVideo(@PathVariable String videoId) {
        try {
            StreamingResponseBody stream = videoService.streamVideo(videoId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.set(HttpHeaders.CACHE_CONTROL, "no-cache");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(stream);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    } 
} 
