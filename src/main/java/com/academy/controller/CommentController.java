package com.academy.controller;

import com.academy.entity.Comment;
import com.academy.entity.Video;
import com.academy.repository.CommentRepository;
import com.academy.repository.VideoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/comments")
public class CommentController {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VideoRepository videoRepository;

    /**
     * Add comment to video
     * Only students can add comments
     */
    @PostMapping("/add")
    public String addComment(
            @RequestParam Long videoId,
            @RequestParam String commentText,
            HttpSession session) throws UnsupportedEncodingException {

        // Check if student is logged in
        Long studentId = (Long) session.getAttribute("studentId");
        String studentName = (String) session.getAttribute("studentName");
        Long teacherId = (Long) session.getAttribute("teacherId");

        if (studentId == null) {
            return "redirect:/student/login";
        }

        // Validate comment
        if (commentText == null || commentText.trim().isEmpty()) {
            return "redirect:/student/videos/" + videoId + "/play?error="
                    + java.net.URLEncoder.encode("التعليق فارغ", "UTF-8");
        }

        if (commentText.length() > 500) {
            return "redirect:/student/videos/" + videoId + "/play?error="
                    + java.net.URLEncoder.encode("التعليق طويل جداً (الحد الأقصى 500 حرف)", "UTF-8");
        }

        // Check if video exists and belongs to student's teacher
        Video video = videoRepository.findById(videoId).orElse(null);
        if (video == null || !video.getTeacherId().equals(teacherId)) {
            return "redirect:/student/videos?error=unauthorized";
        }

        // Create comment
        Comment comment = new Comment();
        comment.setVideoId(videoId);
        comment.setStudentId(studentId);
        comment.setStudentName(studentName);
        comment.setCommentText(commentText.trim());

        commentRepository.save(comment);

        return "redirect:/student/videos/" + videoId + "/play?success="
                + java.net.URLEncoder.encode("تم إضافة التعليق بنجاح", "UTF-8");
    }

    /**
     * Delete comment
     * - Students can delete their own comments
     * - Teachers can delete any comment on their videos
     */
    @PostMapping("/{id}/delete")
    public String deleteComment(
            @PathVariable Long id,
            HttpSession session) throws UnsupportedEncodingException {

        String userType = (String) session.getAttribute("userType");

        // Check authentication
        if (userType == null) {
            return "redirect:/login";
        }

        Comment comment = commentRepository.findById(id).orElse(null);
        if (comment == null) {
            return "redirect:/videos";
        }

        Long videoId = comment.getVideoId();
        Video video = videoRepository.findById(videoId).orElse(null);

        if (video == null) {
            return "redirect:/videos";
        }

        // Authorization logic
        boolean canDelete = false;
        String redirectUrl = "";

        if ("student".equals(userType)) {
            // Student can only delete their own comments
            Long studentId = (Long) session.getAttribute("studentId");
            canDelete = comment.getStudentId().equals(studentId);
            redirectUrl = "/student/videos/" + videoId + "/play";

        } else if ("teacher".equals(userType)) {
            // Teacher can delete any comment on their videos
            Long teacherId = (Long) session.getAttribute("userId");
            canDelete = video.getTeacherId().equals(teacherId);
            redirectUrl = "/videos/" + videoId + "/play";
        }

        if (!canDelete) {
            return "redirect:" + redirectUrl + "?error="
                    + java.net.URLEncoder.encode("غير مصرح لك بحذف هذا التعليق", "UTF-8");
        }

        // Delete comment
        commentRepository.delete(comment);

        return "redirect:" + redirectUrl + "?success="
                + java.net.URLEncoder.encode("تم حذف التعليق", "UTF-8");
    }

    // ========== AJAX ENDPOINTS ==========

    /**
     * Add comment via AJAX (no page reload)
     */
    @PostMapping("/add-ajax")
    @ResponseBody
    public Map<String, Object> addCommentAjax(
            @RequestParam Long videoId,
            @RequestParam String commentText,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Check if student is logged in
            Long studentId = (Long) session.getAttribute("studentId");
            String studentName = (String) session.getAttribute("studentName");
            Long teacherId = (Long) session.getAttribute("teacherId");

            if (studentId == null) {
                response.put("success", false);
                response.put("error", "يجب تسجيل الدخول أولاً");
                return response;
            }

            // Validate comment
            if (commentText == null || commentText.trim().isEmpty()) {
                response.put("success", false);
                response.put("error", "التعليق فارغ");
                return response;
            }

            if (commentText.length() > 500) {
                response.put("success", false);
                response.put("error", "التعليق طويل جداً (الحد الأقصى 500 حرف)");
                return response;
            }

            // Check if video exists and belongs to student's teacher
            Video video = videoRepository.findById(videoId).orElse(null);
            if (video == null || !video.getTeacherId().equals(teacherId)) {
                response.put("success", false);
                response.put("error", "الفيديو غير موجود");
                return response;
            }

            // Create comment
            Comment comment = new Comment();
            comment.setVideoId(videoId);
            comment.setStudentId(studentId);
            comment.setStudentName(studentName);
            comment.setCommentText(commentText.trim());

            Comment savedComment = commentRepository.save(comment);

            // Return success with comment data
            Map<String, Object> commentData = new HashMap<>();
            commentData.put("id", savedComment.getId());
            commentData.put("studentId", savedComment.getStudentId());
            commentData.put("studentName", savedComment.getStudentName());
            commentData.put("commentText", savedComment.getCommentText());
            commentData.put("relativeTime", "الآن");

            response.put("success", true);
            response.put("comment", commentData);

        } catch (Exception e) {
            e.printStackTrace();
            response.put("success", false);
            response.put("error", "حدث خطأ أثناء إضافة التعليق: " + e.getMessage());
        }

        return response;
    }

    /**
     * Delete comment via AJAX (no page reload)
     */
    @PostMapping("/{id}/delete-ajax")
    @ResponseBody
    public Map<String, Object> deleteCommentAjax(
            @PathVariable Long id,
            HttpSession session) {

        Map<String, Object> response = new HashMap<>();

        try {
            String userType = (String) session.getAttribute("userType");

            // Check authentication
            if (userType == null) {
                response.put("success", false);
                response.put("error", "يجب تسجيل الدخول");
                return response;
            }

            Comment comment = commentRepository.findById(id).orElse(null);
            if (comment == null) {
                response.put("success", false);
                response.put("error", "التعليق غير موجود");
                return response;
            }

            Long videoId = comment.getVideoId();
            Video video = videoRepository.findById(videoId).orElse(null);

            if (video == null) {
                response.put("success", false);
                response.put("error", "الفيديو غير موجود");
                return response;
            }

            // Authorization logic
            boolean canDelete = false;

            if ("student".equals(userType)) {
                // Student can only delete their own comments
                Long studentId = (Long) session.getAttribute("studentId");
                canDelete = comment.getStudentId().equals(studentId);

            } else if ("teacher".equals(userType)) {
                // Teacher can delete any comment on their videos
                Long teacherId = (Long) session.getAttribute("userId");
                canDelete = video.getTeacherId().equals(teacherId);
            }

            if (!canDelete) {
                response.put("success", false);
                response.put("error", "غير مصرح لك بحذف هذا التعليق");
                return response;
            }

            // Delete comment
            commentRepository.delete(comment);

            response.put("success", true);

        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "حدث خطأ أثناء حذف التعليق");
        }

        return response;
    }
}