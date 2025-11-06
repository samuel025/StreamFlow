package com.lockedfunds.streaming.entity;

import com.videoStream.streaming.StreamingServiceGrpc;
import com.videoStream.streaming.UploadVideoRequest;
import com.videoStream.streaming.UploadVideoResponse;
import io.grpc.stub.StreamObserver;

public class StreamServiceImpl extends StreamingServiceGrpc.StreamingServiceImplBase {
    @Override
    public StreamObserver<UploadVideoRequest> uploadVideo(StreamObserver<UploadVideoResponse> responseObserver) {
        return super.uploadVideo(responseObserver);
    }
}
