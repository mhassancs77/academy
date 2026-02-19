package com.academy.controller;

import com.academy.dto.VideoSourceType;
import com.academy.entity.Video;
import com.academy.entity.Comment;
import com.academy.repository.VideoRepository;
import com.academy.repository.CommentRepository;
import com.academy.service.FileValidationService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.academy.util.AuthorizationHelper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller

public class VideoController {

    private static final Logger log = LoggerFactory.getLogger(VideoController.class);
    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private FileValidationService fileValidationService;

    private static final String UPLOAD_DIR = "uploads/videos/";

    // ========== LIST VIDEOS ==========
    @GetMapping("/videos")
    public String listVideos(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        List<Video> videos = videoRepository.findByTeacherId(userId);

        model.addAttribute("videos", videos);
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "videos");
        model.addAttribute("pageTitle", "دروسي");
        model.addAttribute("breadcrumbs", "الدروس");

        return "videos-list";
    }

    @GetMapping("/upload-video")
    public String uploadPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "videos");
        model.addAttribute("pageTitle", "رفع درس جديد");
        model.addAttribute("breadcrumbs", "الدروس / رفع درس");

        return "upload-video";
    }

    // ========== UPLOAD ACTION ==========
    // ========== UPLOAD ACTION ==========
// Change signature: file is no longer required, add sourceType + externalUrl
    @PostMapping("/upload-video")
    public String uploadVideo(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam String gradeLevel,
            @RequestParam String sourceType,
            @RequestParam(required = false) String externalUrl,
            HttpSession session,
            Model model) {

        if (!AuthorizationHelper.isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        Long teacherId = AuthorizationHelper.getCurrentTeacherId(session);
        VideoSourceType type = VideoSourceType.valueOf(sourceType.toUpperCase());

        // Validate title first (applies to all types)
        String titleError = fileValidationService.validateText(title, "عنوان الدرس", 100, true);
        if (titleError != null) {
            model.addAttribute("error", titleError);
            model.addAttribute("fullName", session.getAttribute("fullName"));
            return "upload-video";
        }

        if (description != null && !description.trim().isEmpty()) {
            String descError = fileValidationService.validateText(description, "الوصف", 500, false);
            if (descError != null) {
                model.addAttribute("error", descError);
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }
        }

        Video video = new Video();
        video.setTeacherId(teacherId);
        video.setTitle(title.trim());
        video.setDescription(description != null ? description.trim() : null);
        video.setSubject("English");
        video.setGradeLevel(gradeLevel);
        video.setSourceType(type);

        if (type == VideoSourceType.LOCAL) {
            String fileError = fileValidationService.validateVideoFile(file);
            if (fileError != null) {
                model.addAttribute("error", fileError);
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }
            try {
                File uploadDir = new File(UPLOAD_DIR + teacherId);
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String sanitizedName = fileValidationService.sanitizeFilename(file.getOriginalFilename());
                String filename = System.currentTimeMillis() + "_" + sanitizedName;
                Path filePath = Paths.get(UPLOAD_DIR + teacherId + "/" + filename);
                Files.write(filePath, file.getBytes());

                video.setFilePath(filePath.toString());
                video.setFileName(filename);
            } catch (Exception e) {
                log.error("Failed to upload video", e);
                model.addAttribute("error", "فشل رفع الفيديو: " + e.getMessage());
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }

        } else if (type == VideoSourceType.YOUTUBE) {
            // ── YOUTUBE: validate + convert watch URL → embed URL ──
            if (externalUrl == null || externalUrl.trim().isEmpty()) {
                model.addAttribute("error", "يرجى إدخال رابط يوتيوب");
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }

            String embedUrl = convertToYouTubeEmbed(externalUrl.trim());

            if (embedUrl == null) {
                model.addAttribute("error", "رابط يوتيوب غير صالح. يرجى نسخ الرابط من شريط العنوان");
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }

            video.setExternalUrl(embedUrl);  // store the clean embed URL

        } else {
            // GOOGLE_DRIVE / S3 / EXTERNAL
            if (externalUrl == null || externalUrl.trim().isEmpty()) {
                model.addAttribute("error", "يرجى إدخال رابط الفيديو");
                model.addAttribute("fullName", session.getAttribute("fullName"));
                return "upload-video";
            }
            video.setExternalUrl(externalUrl.trim());
        }

        videoRepository.save(video);
        return "redirect:/videos?success=uploaded";
    }


    // ── HELPER: convert any YouTube URL format → clean embed URL ──
    private String convertToYouTubeEmbed(String url) {
        String videoId = extractYouTubeVideoId(url);
        if (videoId == null) return null;
        return "https://www.youtube.com/embed/" + videoId
                + "?rel=0&modestbranding=1&disablekb=1";
    }

    private String extractYouTubeVideoId(String url) {
        // Handles all common YouTube URL formats:
        // https://www.youtube.com/watch?v=VIDEO_ID
        // https://youtu.be/VIDEO_ID
        // https://www.youtube.com/embed/VIDEO_ID        ← already embed, still extract ID
        // https://www.youtube.com/watch?v=VIDEO_ID&t=30s
        // https://m.youtube.com/watch?v=VIDEO_ID        ← mobile

        if (url == null || url.isBlank()) return null;

        try {
            // Pattern covers all formats above
            String pattern =
                    "(?:youtube\\.com/(?:watch\\?.*v=|embed/)|youtu\\.be/)([a-zA-Z0-9_-]{11})";
            java.util.regex.Pattern p =
                    java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(url);

            if (m.find()) {
                return m.group(1);  // the 11-char video ID
            }
        } catch (Exception e) {
            log.warn("Failed to parse YouTube URL: {}", url);
        }

        return null;  // not a valid YouTube URL
    }


    // ========== TEACHER PLAY VIDEO WITH COMMENTS ==========
    @GetMapping("/videos/{id}/play")
    public String playVideo(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        // Check authentication
        if (!AuthorizationHelper.isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        Long teacherId = AuthorizationHelper.getCurrentTeacherId(session);
        String userType = (String) session.getAttribute("userType");

        // Get video
        Video video = videoRepository.findById(id).orElse(null);

        if (video == null) {
            return "redirect:/videos?error=not-found";
        }

        // Check authorization
        if (!AuthorizationHelper.canAccessResource(session, video.getTeacherId())) {
            return "redirect:/videos?error=unauthorized";
        }

        // Get comments for this video
        List<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(id);
        long commentsCount = comments.size();

        // Format comments with relative time
        List<Map<String, Object>> commentsWithTime = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("studentId", comment.getStudentId());
            commentMap.put("studentName", comment.getStudentName());
            commentMap.put("commentText", comment.getCommentText());
            commentMap.put("relativeTime", formatRelativeTime(comment.getCreatedAt()));
            commentMap.put("createdAt", comment.getCreatedAt());
            commentsWithTime.add(commentMap);
        }

        model.addAttribute("video", video);
        model.addAttribute("comments", commentsWithTime);
        model.addAttribute("commentsCount", commentsCount);
        model.addAttribute("teacherId", teacherId);
        model.addAttribute("userType", userType);
        model.addAttribute("fullName", session.getAttribute("fullName"));

        // Use teacher video player template
        return "teacher-video-player";
    }

    // ========== DELETE VIDEO ==========
    @PostMapping("/videos/{id}/delete")
    public String deleteVideo(
            @PathVariable Long id,
            HttpSession session) {

        // Check authentication
        if (!AuthorizationHelper.isAuthenticated(session)) {
            return "redirect:/admin/login";
        }

        Video video = videoRepository.findById(id).orElse(null);

        if (video == null) {
            return "redirect:/videos?error=not-found";
        }

        // Check authorization: teacher can only delete their own videos
        if (!AuthorizationHelper.canAccessResource(session, video.getTeacherId())) {
            return "redirect:/videos?error=unauthorized";
        }

        try {
            // Delete file from disk
            File file = new File(video.getFilePath());
            if (file.exists()) {
                file.delete();
            }

            // Delete from database
            videoRepository.delete(video);

            return "redirect:/videos?success=deleted";

        } catch (Exception e) {
            return "redirect:/videos?error=delete-failed";
        }
    }

    // ========== STREAM VIDEO ==========
    // ========== STREAM VIDEO ==========
// Add one guard: LOCAL only
    @GetMapping("/videos/{id}/stream")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable Long id,
            HttpSession session) {

        if (!AuthorizationHelper.isAuthenticated(session)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Video video = videoRepository.findById(id).orElse(null);
        if (video == null) return ResponseEntity.notFound().build();

        if (!AuthorizationHelper.canAccessResource(session, video.getTeacherId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        // ← NEW: external videos are not served through this endpoint
        if (!video.isLocalVideo()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        try {
            File videoFile = new File(video.getFilePath());
            if (!videoFile.exists()) return ResponseEntity.notFound().build();

            Resource resource = new FileSystemResource(videoFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("video/mp4"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + video.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Format relative time for comments
     */
    private String formatRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = Duration.between(dateTime, now).toMinutes();

        if (minutesDiff < 1) {
            return "الآن";
        } else if (minutesDiff < 60) {
            return "منذ " + minutesDiff + " دقيقة";
        } else if (minutesDiff < 1440) {
            long hours = minutesDiff / 60;
            return "منذ " + hours + " ساعة";
        } else {
            long days = minutesDiff / 1440;
            return "منذ " + days + " يوم";
        }
    }
}