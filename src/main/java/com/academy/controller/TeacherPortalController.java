package com.academy.controller;

import com.academy.entity.User;
import com.academy.entity.Student;
import com.academy.entity.StudentAccount;
import com.academy.repository.UserRepository;
import com.academy.repository.StudentRepository;
import com.academy.repository.StudentAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
public class TeacherPortalController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Teacher Portal Landing Page
     * Shows teacher info and login form for students
     *
     * URL: /teacher/{portalId}
     * Example: /teacher/a7f3b2c9-4d1e-4a8f-9b2c-3e5f6a7b8c9d
     */
    @GetMapping("/teacher/{portalId}")
    public String teacherPortal(
            @PathVariable String portalId,
            HttpSession session,
            Model model) {

        // If student is already logged in, redirect to videos
        if (session.getAttribute("studentId") != null) {
            return "redirect:/student/videos";
        }

        // Find teacher by portal ID
        Optional<User> teacherOpt = userRepository.findByPortalId(portalId);

        if (teacherOpt.isEmpty()) {
            model.addAttribute("error", "المعلم غير موجود");
            return "teacher-not-found";
        }

        User teacher = teacherOpt.get();

        // Add teacher info to model
        model.addAttribute("teacherId", teacher.getId());
        model.addAttribute("portalId", teacher.getPortalId());
        model.addAttribute("teacherName", teacher.getFullName());
        model.addAttribute("teacherSubject", teacher.getSubject());

        return "teacher-portal";
    }

    /**
     * Student Login Through Teacher Portal
     */
    @PostMapping("/teacher/{portalId}/login")
    public String teacherPortalLogin(
            @PathVariable String portalId,
            @RequestParam String username,
            @RequestParam String password,
            Model model,
            HttpSession session) {

        // 1️⃣ Verify teacher exists by portal ID
        Optional<User> teacherOpt = userRepository.findByPortalId(portalId);

        if (teacherOpt.isEmpty()) {
            model.addAttribute("error", "رابط المعلم غير صالح");
            return "teacher-not-found";
        }

        User teacher = teacherOpt.get();

        // 2️⃣ Find student account by username
        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findByUsername(username);

        if (accountOpt.isEmpty()) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            addTeacherInfoToModel(teacher, portalId, model);
            return "teacher-portal";
        }

        StudentAccount account = accountOpt.get();

        // 3️⃣ Verify password
        if (!passwordEncoder.matches(password, account.getPassword())) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            addTeacherInfoToModel(teacher, portalId, model);
            return "teacher-portal";
        }

        // 4️⃣ Check if account is active
        if (!account.getIsActive()) {
            model.addAttribute("error", "الحساب غير نشط. تواصل مع المعلم");
            addTeacherInfoToModel(teacher, portalId, model);
            return "teacher-portal";
        }

        // 5️⃣ Get student info
        Optional<Student> studentOpt =
                studentRepository.findById(account.getStudentId());

        if (studentOpt.isEmpty()) {
            model.addAttribute("error", "خطأ في تحميل بيانات الطالب");
            addTeacherInfoToModel(teacher, portalId, model);
            return "teacher-portal";
        }

        Student student = studentOpt.get();

        // 6️⃣ CRITICAL: Verify student belongs to THIS teacher
        if (!student.getTeacherId().equals(teacher.getId())) {
            model.addAttribute("error", "هذا الحساب لا ينتمي لهذا المعلم");
            addTeacherInfoToModel(teacher, portalId, model);
            return "teacher-portal";
        }

        // 7️⃣ Update last login time
        account.setLastLogin(LocalDateTime.now());
        studentAccountRepository.save(account);

        // 8️⃣ Store complete session data
        session.setAttribute("studentAccountId", account.getId());
        session.setAttribute("studentId", student.getId());
        session.setAttribute("studentName", student.getFullName());
        session.setAttribute("studentGrade", student.getGradeLevel());
        session.setAttribute("teacherId", teacher.getId());
        session.setAttribute("teacherPortalId", portalId);
        session.setAttribute("userType", "student");

        // 9️⃣ Redirect to student videos
        return "redirect:/student/videos";
    }

    /**
     * Get Teacher Portal Link (for teacher to share)
     * Shows in dashboard
     */
    @GetMapping("/my-portal-link")
    public String myPortalLink(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return "redirect:/login";
        }

        User teacher = userRepository.findById(userId).orElse(null);

        if (teacher == null) {
            return "redirect:/login";
        }

        // Generate portal link
        String portalLink = "/teacher/" + teacher.getPortalId();

        model.addAttribute("teacherName", teacher.getFullName());
        model.addAttribute("portalLink", portalLink);
        model.addAttribute("fullLink", "http://localhost:8080" + portalLink);

        return "my-portal-link";
    }

    /**
     * Helper method to add teacher info to model
     */
    private void addTeacherInfoToModel(User teacher, String portalId, Model model) {
        model.addAttribute("teacherId", teacher.getId());
        model.addAttribute("portalId", portalId);
        model.addAttribute("teacherName", teacher.getFullName());
        model.addAttribute("teacherSubject", teacher.getSubject());
    }
}