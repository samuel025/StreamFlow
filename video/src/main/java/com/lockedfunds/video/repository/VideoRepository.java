package com.lockedfunds.video.repository;

import com.lockedfunds.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VideoRepository extends JpaRepository<Video, Long> {

    boolean existsByVideoId(String videoId);
}
