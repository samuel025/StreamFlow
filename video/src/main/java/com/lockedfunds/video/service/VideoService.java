package com.lockedfunds.video.service;

import com.lockedfunds.video.dto.DataResponseDTO;
import com.lockedfunds.video.dto.VideoDTO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.concurrent.CompletableFuture;

@Service
public interface VideoService {
    CompletableFuture<DataResponseDTO> uploadVideo(VideoDTO video, MultipartFile file);
    StreamingResponseBody streamVideo(String videoId);
}
