package com.academy.repository;

import com.academy.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Find all comments for a specific video, ordered by newest first
     */
    List<Comment> findByVideoIdOrderByCreatedAtDesc(Long videoId);

    /**
     * Count comments for a video
     */
    long countByVideoId(Long videoId);

    /**
     * Find comments by student
     */
    List<Comment> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    long countByStudentId(Long studentId);
}