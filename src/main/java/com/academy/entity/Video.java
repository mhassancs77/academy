package com.academy.entity;

import com.academy.dto.VideoSourceType;
import jakarta.persistence.*;
//import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "videos")
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(name = "file_path", length = 500)
    private String filePath;              // nullable now — only used for LOCAL

    @Column(name = "file_name")
    private String fileName;              // nullable now — only used for LOCAL

    @Column(name = "external_url", length = 1000)
    private String externalUrl;           // ← NEW: for YouTube/Drive/S3/External URLs

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = true, length = 20)
    private VideoSourceType sourceType ; //= VideoSourceType.LOCAL;  // ← NEW: defaults to LOCAL

    @Column(name = "grade_level", length = 50)
    private String gradeLevel;

    @Column(name = "subject")
    private String subject;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


    //todo: will remove the getters/setters and use  lombok

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(String gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }



    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSubject() {
        return subject;
    }

    public VideoSourceType getSourceType() { return sourceType; }
    public void setSourceType(VideoSourceType sourceType) { this.sourceType = sourceType; }

    public String getExternalUrl() { return externalUrl; }
    public void setExternalUrl(String externalUrl) { this.externalUrl = externalUrl; }

    // ← Useful helper to avoid null checks everywhere in templates/controllers
    public boolean isLocalVideo() {
        return sourceType == VideoSourceType.LOCAL;
    }

    public boolean isYouTubeVideo() {
        return sourceType == VideoSourceType.YOUTUBE;
    }

    // The "playable" URL — controllers/templates call this instead of checking type
    public String getPlayableUrl() {
        if (sourceType == VideoSourceType.LOCAL) {
            return "/videos/" + id + "/stream";
        }
        return externalUrl; // YouTube, Drive, S3, etc.
    }
}