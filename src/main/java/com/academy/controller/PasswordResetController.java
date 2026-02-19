package com.academy.controller;

import com.academy.entity.StudentAccount;
import com.academy.repository.StudentAccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
@RequestMapping("/password-reset")
public class PasswordResetController {

    @Autowired
    private StudentAccountRepository studentAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Teacher: View password reset page for students
     */
    @GetMapping
    public String passwordResetPage(HttpSession session, Model model) {
        // Check if teacher is logged in
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "students");
        return "password-reset";
    }

    /**
     * Teacher: Search for student account by username
     */
    @PostMapping("/search")
    public String searchStudent(
            @RequestParam String username,
            HttpSession session,
            Model model) {

        // Check if teacher is logged in
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) {
            return "redirect:/admin/login";
        }

        // Find student account
        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findByUsername(username);

        if (accountOpt.isEmpty()) {
            model.addAttribute("error", "لم يتم العثور على حساب بهذا الاسم");
            model.addAttribute("fullName", session.getAttribute("fullName"));
            return "password-reset";
        }

        StudentAccount account = accountOpt.get();

        // Add account info to model
        model.addAttribute("account", account);
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "students");

        return "password-reset";
    }

    /**
     * Teacher: Reset student password
     */
    @PostMapping("/reset")
    public String resetPassword(
            @RequestParam Long accountId,
            @RequestParam String newPassword,
            @RequestParam String confirmPassword,
            HttpSession session,
            Model model) {

        // Check if teacher is logged in
        Long teacherId = (Long) session.getAttribute("userId");
        if (teacherId == null) {
            return "redirect:/admin/login";
        }

        // Validate passwords match
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "كلمات المرور غير متطابقة");
            model.addAttribute("fullName", session.getAttribute("fullName"));

            // Reload account info
            Optional<StudentAccount> accountOpt =
                    studentAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                model.addAttribute("account", accountOpt.get());
            }

            return "password-reset";
        }

        // Validate password strength
        if (newPassword.length() < 6) {
            model.addAttribute("error", "كلمة المرور يجب أن تكون 6 أحرف على الأقل");
            model.addAttribute("fullName", session.getAttribute("fullName"));

            Optional<StudentAccount> accountOpt =
                    studentAccountRepository.findById(accountId);
            if (accountOpt.isPresent()) {
                model.addAttribute("account", accountOpt.get());
            }

            return "password-reset";
        }

        // Get account
        Optional<StudentAccount> accountOpt =
                studentAccountRepository.findById(accountId);

        if (accountOpt.isEmpty()) {
            model.addAttribute("error", "الحساب غير موجود");
            model.addAttribute("fullName", session.getAttribute("fullName"));
            return "password-reset";
        }

        StudentAccount account = accountOpt.get();

        // Update password
        account.setPassword(passwordEncoder.encode(newPassword));
        studentAccountRepository.save(account);

        // Success message
        model.addAttribute("success", "تم تغيير كلمة المرور بنجاح");
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", "students");

        return "password-reset";
    }
}