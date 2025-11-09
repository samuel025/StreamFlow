package com.lockedfunds.video.service;

import com.example.streaming.StreamingServiceGrpc;
import com.example.streaming.StreamVideoRequest;
import com.example.streaming.StreamVideoResponse;
import com.example.streaming.UploadVideoRequest;
import com.example.streaming.UploadVideoResponse;
import com.google.protobuf.ByteString;
import com.lockedfunds.video.dto.DataResponseDTO;
import com.lockedfunds.video.dto.VideoDTO;
import com.lockedfunds.video.entity.Video;
import com.lockedfunds.video.repository.VideoRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;

    @GrpcClient("streaming-service")
    private StreamingServiceGrpc.StreamingServiceStub streamingServiceStub;

    @GrpcClient("streaming-service")
    private StreamingServiceGrpc.StreamingServiceBlockingStub streamingServiceBlockingStub;

    @Override
    public CompletableFuture<DataResponseDTO> uploadVideo(VideoDTO video, MultipartFile file) {

        String videoID;
        do {
            videoID = generateVideoId();
        } while (videoRepository.existsByVideoId(videoID));



        CompletableFuture<UploadVideoResponse> future = new CompletableFuture<>();

        StreamObserver<UploadVideoResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(UploadVideoResponse response) {
                future.complete(response);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onCompleted() {
            }
        };

        StreamObserver<UploadVideoRequest> requestObserver = streamingServiceStub.uploadVideo(responseObserver);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1024 * 1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                UploadVideoRequest request = UploadVideoRequest.newBuilder()
                        .setVideoId(videoID)
                        .setChunkData(ByteString.copyFrom(buffer, 0, bytesRead))
                        .setChunkSize(bytesRead)
                        .setIsLastChunk(false)
                        .build();
                requestObserver.onNext(request);
            }

            UploadVideoRequest lastChunk = UploadVideoRequest.newBuilder()
                    .setVideoId(videoID)
                    .setIsLastChunk(true)
                    .build();
            requestObserver.onNext(lastChunk);
            requestObserver.onCompleted();

            Video videoEntity = Video.builder()
                    .createdAt(Instant.now())
                    .title(video.getTitle())
                    .description(video.getDescription())
                    .videoId(videoID)
                    .build();
            videoRepository.save(videoEntity);

        } catch (IOException e) {
            requestObserver.onError(e);
            future.completeExceptionally(e);
        }

        return future.thenApply(response -> DataResponseDTO.builder()
                .status("200")
                .message("Upload completed: " + response.getStoragePath())
                .build()
        );
    }

    @Override
    public StreamingResponseBody streamVideo(String videoId) {
        return outputStream -> {
            try {
                log.info("🎬 Requesting video stream for: {}", videoId);

                StreamVideoRequest request = StreamVideoRequest.newBuilder()
                        .setVideoId(videoId)
                        .build();

                Iterator<StreamVideoResponse> responseIterator = 
                    streamingServiceBlockingStub.streamVideo(request);

                long totalBytes = 0;
                while (responseIterator.hasNext()) {
                    StreamVideoResponse response = responseIterator.next();
                    byte[] chunkData = response.getChunkData().toByteArray();
                    outputStream.write(chunkData);
                    totalBytes += chunkData.length;
                    
                    log.debug("📥 Received chunk: {} bytes (total: {})", 
                        chunkData.length, totalBytes);

                    if (response.getIsLastChunk()) {
                        log.info("✅ Video streaming completed: {} ({} bytes)", 
                            videoId, totalBytes);
                        break;
                    }
                }

                outputStream.flush();
            } catch (Exception e) {
                log.error("❌ Error streaming video: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to stream video: " + e.getMessage(), e);
            }
        };
    }

    public String generateVideoId() {
        long timestamp = Instant.now().toEpochMilli();
        int random = new Random().nextInt(900) + 100;
        return "VID-" + timestamp + random;
    }
}
