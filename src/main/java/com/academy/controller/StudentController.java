package com.academy.controller;

import com.academy.entity.Student;
import com.academy.entity.StudentAccount;
import com.academy.repository.StudentRepository;
import com.academy.repository.StudentAccountRepository;
import com.academy.util.GradeLevelUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Controller
public class StudentController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;



// ========================================
// IMPORTANT: Add these attributes to ALL controller methods
    // added thid attributes to ther controllers
// ========================================

    /**
     * Standard attributes to add to every page:
     *
     * model.addAttribute("fullName", session.getAttribute("fullName"));
     * model.addAttribute("activePage", "page_name");  // dashboard, videos, students, etc.
     * model.addAttribute("pageTitle", "Page Title");
     * model.addAttribute("breadcrumbs", "Breadcrumb");
     */

    // ========== LIST ALL STUDENTS ==========
    @GetMapping("/students")
    public String listStudents(HttpSession session, Model model) {
        // Check authentication
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        // Get all students for this teacher
        List<Student> students = studentRepository.findByTeacherId(userId);

        //todo will revisit the part of student has account

        // Format grades for display
        students.forEach(student -> {
            Optional<StudentAccount> existingAccount =
                    studentAccountRepository.findByStudentId(student.getId());
            if (existingAccount.isPresent()) {
                student.setHasAccount(true);
            }
            String gradeName = GradeLevelUtil.getGradeName(student.getGradeLevel());
            student.setGradeLevel(gradeName);
        });

        model.addAttribute("students", students);
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "students");

        return "students-list";
    }


    // ========== ADD STUDENT PAGE ==========
    @GetMapping("/students/add")
    public String addStudentPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/loginn";
        }

        model.addAttribute("fullName", session.getAttribute("fullName"));
        return "add-student";
    }

    // ========== ADD STUDENT ACTION ==========
    @PostMapping("/students/add")
    public String addStudent(
            @RequestParam String fullName,
            @RequestParam String phone,
            @RequestParam String parentPhone,
            @RequestParam String gradeLevel,
            @RequestParam(required = false) String notes,
            HttpSession session) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        // Create and save student
        Student student = new Student();
        student.setTeacherId(userId);
        student.setFullName(fullName);
        student.setPhone(phone);
        student.setParentPhone(parentPhone);
        student.setGradeLevel(gradeLevel);
        student.setNotes(notes);

        studentRepository.save(student);

        return "redirect:/students";
    }

    // ========== CREATE ACCOUNT PAGE ==========
    @GetMapping("/students/{id}/create-account")
    public String createAccountPage(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        Student student = studentRepository.findById(id).orElse(null);
        if (student == null || !student.getTeacherId().equals(userId)) {
            return "redirect:/students";
        }

        // Check if account already exists
        Optional<StudentAccount> existingAccount =
                studentAccountRepository.findByStudentId(id);
        if (existingAccount.isPresent()) {
            return "redirect:/students?error=account-exists";
        }

        // Generate suggested username (first name + random number)
        String suggestedUsername = generateUsername(student.getFullName());

        model.addAttribute("student", student);
        model.addAttribute("suggestedUsername", suggestedUsername);
        model.addAttribute("fullName", session.getAttribute("fullName"));

        return "create-student-account";
    }

    // ========== CREATE ACCOUNT ACTION ==========
    @PostMapping("/students/{id}/create-account")
    public String createAccount(
            @PathVariable Long id,
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        Student student = studentRepository.findById(id).orElse(null);
        if (student == null || !student.getTeacherId().equals(userId)) {
            return "redirect:/students";
        }

        // Check if username exists
        if (studentAccountRepository.existsByUsername(username)) {
            model.addAttribute("error", "اسم المستخدم موجود بالفعل، اختر اسم آخر");
            model.addAttribute("student", student);
            String suggestedUsername = generateUsername(student.getFullName());
            model.addAttribute("suggestedUsername", suggestedUsername);
            return "create-student-account";
        }

        // Create account
        StudentAccount account = new StudentAccount();
        account.setStudentId(id);
        account.setUsername(username);
        account.setPassword(passwordEncoder.encode(password)); // ← Hash password!
        account.setIsActive(true);

        studentAccountRepository.save(account);

        // Store created account info in session to show it
        session.setAttribute("createdUsername", username);
        session.setAttribute("createdPassword", password); // Store plain password to show once
        session.setAttribute("createdStudentName", student.getFullName());

        return "redirect:/students/" + id + "/account";
    }

    // ========== VIEW ACCOUNT DETAILS ==========
    @GetMapping("/students/{id}/account")
    public String viewAccount(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        Student student = studentRepository.findById(id).orElse(null);
        if (student == null || !student.getTeacherId().equals(userId)) {
            return "redirect:/students";
        }

        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findByStudentId(id);

        if (accountOpt.isEmpty()) {
            return "redirect:/students/{id}/create-account";
        }

        StudentAccount account = accountOpt.get();

        model.addAttribute("student", student);
        model.addAttribute("account", account);
        model.addAttribute("fullName", session.getAttribute("fullName"));

        // Check if this is right after creating account
        String createdUsername = (String) session.getAttribute("createdUsername");
        if (createdUsername != null) {
            model.addAttribute("justCreated", true);
            model.addAttribute("createdUsername", createdUsername);
            model.addAttribute("createdPassword", session.getAttribute("createdPassword"));

            // Clear from session after showing once
            session.removeAttribute("createdUsername");
            session.removeAttribute("createdPassword");
            session.removeAttribute("createdStudentName");
        }

        return "view-student-account";
    }

    // ========== HELPER: Generate Username ==========
    private String generateUsername(String fullName) {
        // Take first name and add random 3 digits
        String firstName = fullName.split(" ")[0].toLowerCase();
        int random = (int) (Math.random() * 1000);
        return firstName + random;
    }
}