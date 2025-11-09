package com.lockedfunds.streaming.repository;

import com.lockedfunds.streaming.entity.videoDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideoDetailsRepository extends JpaRepository<videoDetail, Long> {
    Optional<videoDetail> findByVideoId(String videoId);
}
