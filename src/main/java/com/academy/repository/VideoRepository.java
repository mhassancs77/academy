package com.academy.repository;

import com.academy.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    List<Video> findByTeacherId(Long teacherId);
    // For statistics
    long countByTeacherId(Long teacherId);

    // For recent activities
    List<Video> findTop5ByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}