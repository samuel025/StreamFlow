package com.lockedfunds.streaming.service;


import com.google.protobuf.ByteString;
import com.lockedfunds.streaming.entity.videoDetail;
import com.lockedfunds.streaming.repository.VideoDetailsRepository;
import com.videoStream.streaming.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


@GrpcService
@Service
@Slf4j
@RequiredArgsConstructor
public class VideoStreamingService extends StreamingServiceGrpc.StreamingServiceImplBase {

    private static final String STORAGE_DIR = "videos/";
    private static final int CHUNK_SIZE = 1024 * 256; // 256KB chunks
    private final VideoDetailsRepository videoDetailsRepository;

    @Override
    public StreamObserver<UploadVideoRequest> uploadVideo(StreamObserver<UploadVideoResponse> responseObserver) {
        return new StreamObserver<>() {

            FileOutputStream outputStream;
            Path filePath;
            long totalBytes = 0;
            String videoId;

            @Override
            public void onNext(UploadVideoRequest request) {
                try {
                    if (videoId == null) {
                        videoId = request.getVideoId();
                        filePath = Paths.get(STORAGE_DIR, videoId + ".mp4");
                        Files.createDirectories(filePath.getParent());
                        outputStream = new FileOutputStream(filePath.toFile());
                        log.info("📁 Created file: {}", filePath.toAbsolutePath());
                    }

                    ByteString chunkData = request.getChunkData();
                    if (!chunkData.isEmpty()) {
                        byte[] bytes = chunkData.toByteArray();
                        outputStream.write(bytes);
                        totalBytes += bytes.length;
                        log.debug("📦 Wrote chunk: {} bytes (total: {})", bytes.length, totalBytes);
                    }


                    if (request.getIsLastChunk()) {
                        outputStream.flush();
                        outputStream.close();
                        outputStream = null;
                        log.info("✅ Closed file stream for: {}", videoId);
                    }

                } catch (IOException e) {
                    log.error("❌ Error writing video chunk: {}", e.getMessage(), e);
                    responseObserver.onError(io.grpc.Status.INTERNAL
                            .withDescription("Failed to write video data: " + e.getMessage())
                            .withCause(e)
                            .asRuntimeException());
                    cleanup();
                }
            }

            private void cleanup() {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                    if (filePath != null && Files.exists(filePath)) {
                        Files.delete(filePath);
                        log.info("🗑️ Cleaned up partial file: {}", filePath);
                    }
                } catch (IOException e) {
                    log.error("Failed to cleanup: {}", e.getMessage());
                }
            }

            @Override
            public void onError(Throwable throwable) {
                log.error("❌ Upload error: {}", throwable.getMessage());
                try {
                    if (outputStream != null) outputStream.close();
                } catch (IOException ignored) {}
            }

            @Override
            public void onCompleted() {
                videoDetail videoDetailEntity = videoDetail.builder()
                        .videoId(videoId)
                        .fileSize(String.valueOf(totalBytes))
                        .storagePath(filePath.toString())
                        .build();
                videoDetailsRepository.save(videoDetailEntity);
                UploadVideoResponse response = UploadVideoResponse.newBuilder()
                        .setVideoId(videoId)
                        .setStoragePath(filePath.toString())
                        .setTotalSize(totalBytes)
                        .setMessage("✅ Video uploaded successfully")
                        .build();

                responseObserver.onNext(response);
                responseObserver.onCompleted();
                log.info("✅ Upload completed for video: {}", videoId);
            }
        };
    }

    @Override
    public void streamVideo(StreamVideoRequest request, StreamObserver<StreamVideoResponse> responseObserver) {
        String videoId = request.getVideoId();
        Path videoPath = Paths.get(STORAGE_DIR, videoId + ".mp4");

        log.info("🎬 Starting video stream for: {}", videoId);

        videoDetail video = videoDetailsRepository.findByVideoId(videoId)
                .orElse(null);

        if (video == null || !Files.exists(videoPath)) {
            log.error("❌ Video not found: {}", videoId);
            responseObserver.onError(io.grpc.Status.NOT_FOUND
                    .withDescription("Video not found: " + videoId)
                    .asRuntimeException());
            return;
        }

        try (FileInputStream inputStream = new FileInputStream(videoPath.toFile())) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            long totalBytesSent = 0;
            long fileSize = Files.size(videoPath);

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesSent += bytesRead;
                boolean isLastChunk = totalBytesSent >= fileSize;

                StreamVideoResponse response = StreamVideoResponse.newBuilder()
                        .setChunkData(ByteString.copyFrom(buffer, 0, bytesRead))
                        .setChunkSize(bytesRead)
                        .setIsLastChunk(isLastChunk)
                        .build();

                responseObserver.onNext(response);
                log.debug("📤 Sent chunk: {} bytes (total: {}/{})", bytesRead, totalBytesSent, fileSize);
            }

            responseObserver.onCompleted();
            log.info("✅ Video streaming completed: {} ({} bytes)", videoId, totalBytesSent);

        } catch (IOException e) {
            log.error("❌ Error streaming video: {}", e.getMessage(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Failed to stream video: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}

