package com.academy.controller;

import com.academy.entity.User;
import com.academy.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Main controller for public and admin routes
 */
@Controller
public class MainController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * Landing page (public) - Shows teacher info and student login
     * This is now the main entry point for the platform
     */
    @GetMapping("/")
    public String landingPage() {
        return "index";
    }

    /**
     * Hidden admin login page for teacher
     * URL: /admin/login
     * NOT linked from anywhere public
     */
    @GetMapping("/admin/login")
    public String adminLoginPage(HttpSession session) {
        // If already logged in, redirect to dashboard
        if (session.getAttribute("userId") != null) {
            return "redirect:/dashboard";
        }
        return "admin-login";
    }

    /**
     * Admin login action (teacher login)
     */
    @PostMapping("/admin/login")
    public String adminLogin(
            @RequestParam String username,
            @RequestParam String password,
            HttpSession session,
            Model model) {

        // Find user (teacher) by username
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            return "admin-login";
        }

        User user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
            return "admin-login";
        }

        // Store in session
        session.setAttribute("userId", user.getId());
        session.setAttribute("fullName", user.getFullName());
        session.setAttribute("userType", "teacher");

        return "redirect:/dashboard";
    }

    /**
     * Logout for both teacher and students
     */
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        String userType = (String) session.getAttribute("userType");
        session.invalidate();

        // Redirect based on user type
        if ("student".equals(userType)) {
            return "redirect:/student/login";
        } else {
            return "redirect:/admin/login";
        }
    }

    /**
     * Redirect old /login to /admin/login for backward compatibility
     */
    @GetMapping("/login")
    public String oldLoginRedirect() {
        return "redirect:/admin/login";
    }
}