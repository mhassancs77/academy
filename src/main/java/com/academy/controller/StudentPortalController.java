package com.academy.controller;

import com.academy.entity.Comment;
import com.academy.entity.Student;
import com.academy.entity.StudentAccount;
import com.academy.entity.Video;
import com.academy.repository.CommentRepository;
import com.academy.repository.StudentAccountRepository;
import com.academy.repository.StudentRepository;
import com.academy.repository.VideoRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("/student")
public class StudentPortalController {

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CommentRepository commentRepository;


    // ========== STUDENT LOGIN PAGE ==========
    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        // If already logged in, redirect to videos
        if (session.getAttribute("studentId") != null) {
            return "redirect:/student/videos";
        }
        return "student-login";
    }

    // ========== STUDENT LOGIN ACTION ==========
    @PostMapping("/login")
    public String login(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Find account by username only
        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findByUsername(username);

        if (accountOpt.isEmpty()) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            return "student-login";
        }

        StudentAccount account = accountOpt.get();

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, account.getPassword())) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            return "student-login";
        }

        // Check if account is active
        if (!account.getIsActive()) {
            model.addAttribute("error", "الحساب غير نشط. تواصل مع المدرس");
            return "student-login";
        }

        // Update last login time
        account.setLastLogin(LocalDateTime.now());
        studentAccountRepository.save(account);

        // Get student info
        Optional<Student> studentOpt =
                studentRepository.findById(account.getStudentId());

        if (studentOpt.isEmpty()) {
            model.addAttribute("error", "خطأ في تحميل بيانات الطالب");
            return "student-login";
        }

        Student student = studentOpt.get();

        // Store in session
        session.setAttribute("studentAccountId", account.getId());
        session.setAttribute("studentId", student.getId());
        session.setAttribute("studentName", student.getFullName());
        session.setAttribute("studentGrade", student.getGradeLevel());
        session.setAttribute("teacherId", student.getTeacherId());
        session.setAttribute("userType", "student"); // Important for identifying user type

        return "redirect:/student/dashboard";
    }

    // ========== STUDENT DASHBOARD ==========
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        // Check if logged in
        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        Long teacherId = (Long) session.getAttribute("teacherId");
        String studentGrade = (String) session.getAttribute("studentGrade");

        // Get video count for student's grade
        List<Video> allVideos = videoRepository.findByTeacherId(teacherId);
        long videoCount = allVideos.stream()
                .filter(v -> v.getGradeLevel() != null &&
                        v.getGradeLevel().equals(studentGrade))
                .count();

        // Get comment count for this student
        long commentCount = commentRepository.countByStudentId(studentId);

        model.addAttribute("studentName", session.getAttribute("studentName"));
        model.addAttribute("studentGrade", studentGrade);
        model.addAttribute("videoCount", videoCount);
        model.addAttribute("commentCount", commentCount);

        return "student-dashboard";
    }

    // ========== STUDENT LOGOUT ==========
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/student/login";
    }

    // ========== STUDENT VIDEOS LIST ==========
    @GetMapping("/videos")
    public String videos(HttpSession session, Model model) {
        // Check if logged in
        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        Long teacherId = (Long) session.getAttribute("teacherId");
        String studentGrade = (String) session.getAttribute("studentGrade");

        // Get all videos from the teacher
        List<Video> allVideos = videoRepository.findByTeacherId(teacherId);

        // Filter by student's grade level
        List<Video> gradeVideos = allVideos.stream()
                .filter(v -> v.getGradeLevel() != null &&
                        v.getGradeLevel().equals(studentGrade))
                .toList();

        model.addAttribute("videos", gradeVideos);
        model.addAttribute("studentName", session.getAttribute("studentName"));
        model.addAttribute("studentGrade", studentGrade);

        return "student-videos";
    }


    // ========== STREAM VIDEO ==========
// ========== STREAM VIDEO ==========
    @GetMapping("/videos/{id}/stream")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable Long id,
            HttpSession session) {

        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long teacherId = (Long) session.getAttribute("teacherId");
        Video video = videoRepository.findById(id).orElse(null);

        if (video == null || !video.getTeacherId().equals(teacherId)) {
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

    // ========== TEACHER-SPECIFIC STUDENT LOGIN ==========
    @PostMapping("/teacher/{portalId}/login")
    public String teacherStudentLogin(
            @PathVariable String portalId,
            @RequestParam Long teacherId,
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Find account by username only
        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findByUsername(username);

        if (accountOpt.isEmpty()) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            // Pass teacher info back to page
            addTeacherInfoToModel(teacherId, model);
            return "teacher-portal";
        }

        StudentAccount account = accountOpt.get();

        // Verify password using BCrypt
        if (!passwordEncoder.matches(password, account.getPassword())) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            addTeacherInfoToModel(teacherId, model);
            return "teacher-portal";
        }

        // Check if account is active
        if (!account.getIsActive()) {
            model.addAttribute("error", "الحساب غير نشط. تواصل مع المعلم");
            addTeacherInfoToModel(teacherId, model);
            return "teacher-portal";
        }

        // Get student info
        Optional<Student> studentOpt =
                studentRepository.findById(account.getStudentId());

        if (studentOpt.isEmpty()) {
            model.addAttribute("error", "خطأ في تحميل بيانات الطالب");
            addTeacherInfoToModel(teacherId, model);
            return "teacher-portal";
        }

        Student student = studentOpt.get();

        // IMPORTANT: Verify student belongs to this teacher
        if (!student.getTeacherId().equals(teacherId)) {
            model.addAttribute("error", "هذا الحساب لا ينتمي لهذا المعلم");
            addTeacherInfoToModel(teacherId, model);
            return "teacher-portal";
        }

        // Update last login time
        account.setLastLogin(LocalDateTime.now());
        studentAccountRepository.save(account);

        // Store in session
        session.setAttribute("studentAccountId", account.getId());
        session.setAttribute("studentId", student.getId());
        session.setAttribute("studentName", student.getFullName());
        session.setAttribute("studentGrade", student.getGradeLevel()); // ← Auto from DB
        session.setAttribute("teacherId", student.getTeacherId());
        session.setAttribute("teacherPortalId", portalId);
        session.setAttribute("userType", "student");

        return "redirect:/student/videos";
    }

    // Helper method to add teacher info to model
    private void addTeacherInfoToModel(Long teacherId, Model model) {
        // Reuse existing teacher info if available
        if (!model.containsAttribute("teacherId")) {
            // This shouldn't happen normally, but as a fallback
            model.addAttribute("error", "خطأ في تحميل بيانات المعلم");
        }
    }

    // ========== STUDENT VIDEO PLAYER WITH COMMENTS ==========
    @GetMapping("/videos/{id}/play")
    public String playVideo(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        // Check if logged in
        Long studentId = (Long) session.getAttribute("studentId");
        if (studentId == null) {
            return "redirect:/student/login";
        }

        Long teacherId = (Long) session.getAttribute("teacherId");

        // Get video
        Video video = videoRepository.findById(id).orElse(null);

        // Validate: Video exists and belongs to student's teacher
        if (video == null) {
            return "redirect:/student/videos?error=video-not-found";
        }

        if (!video.getTeacherId().equals(teacherId)) {
            return "redirect:/student/videos?error=unauthorized";
        }

        // Get comments for this video
        List<Comment> comments = commentRepository.findByVideoIdOrderByCreatedAtDesc(id);
        long commentsCount = comments.size();

        // Add relative time to comments
        List<Map<String, Object>> commentsWithTime = new ArrayList<>();
        for (Comment comment : comments) {
            Map<String, Object> commentMap = new HashMap<>();
            commentMap.put("id", comment.getId());
            commentMap.put("studentId", comment.getStudentId());
            commentMap.put("studentName", comment.getStudentName());
            commentMap.put("commentText", comment.getCommentText());
            commentMap.put("relativeTime", formatRelativeTime(comment.getCreatedAt()));
            commentsWithTime.add(commentMap);
        }

        model.addAttribute("video", video);
        model.addAttribute("studentName", session.getAttribute("studentName"));
        model.addAttribute("studentId", studentId);
        model.addAttribute("comments", commentsWithTime);
        model.addAttribute("commentsCount", commentsCount);

        return "student-video-player";
    }

    /**
     * Format relative time for comments
     */
    private String formatRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutesDiff = java.time.Duration.between(dateTime, now).toMinutes();

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