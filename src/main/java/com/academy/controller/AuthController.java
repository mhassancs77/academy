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

//@Controller
public class AuthController {
//
//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private PasswordEncoder passwordEncoder;
//
//    // ========== REGISTER PAGE ==========
//    @GetMapping("/register")
//    public String registerPage() {
//        return "register";
//    }
//
//    // ========== REGISTER ACTION ==========
//    @PostMapping("/register")
//    public String register(
//            @RequestParam String username,
//            @RequestParam String password,
//            @RequestParam String confirmPassword,
//            @RequestParam String fullName,
//            @RequestParam(required = false) String phone,
////            @RequestParam String subject,
//            Model model) {
//
//        // Check if passwords match
//        if (!password.equals(confirmPassword)) {
//            model.addAttribute("error", "كلمتا المرور غير متطابقتين");
//            return "register";
//        }
//
//        // Check if username already exists
//        if (userRepository.existsByUsername(username)) {
//            model.addAttribute("error", "اسم المستخدم مستخدم بالفعل");
//            return "register";
//        }
//
//        // Validate password length
//        if (password.length() < 6) {
//            model.addAttribute("error", "كلمة المرور يجب أن تكون 6 أحرف على الأقل");
//            return "register";
//        }
//
//        // Create new user
//        User user = new User();
//        user.setUsername(username);
//        user.setPortalId(java.util.UUID.randomUUID().toString()); // ← Generate unique portal ID
//        user.setPassword(passwordEncoder.encode(password)); // ← Hash password!
//        user.setFullName(fullName);
//        user.setPhone(phone);
//        user.setSubject("English");
//
//        userRepository.save(user);
//
//        return "redirect:/login?registered=success";
//    }
//
//    // ========== LOGIN PAGE ==========
//    @GetMapping("/login")
//    public String loginPage(HttpSession session, Model model) {
//        // If already logged in, redirect to dashboard
//        if (session.getAttribute("userId") != null) {
//            return "redirect:/dashboard";
//        }
//        return "login";
//    }
//
//    // ========== LOGIN ACTION ==========
//    @PostMapping("/login")
//    public String login(
//            @RequestParam String username,
//            @RequestParam String password,
//            HttpSession session,
//            Model model) {
//
//        // Find user by username
//        Optional<User> userOpt = userRepository.findByUsername(username);
//
//        if (userOpt.isEmpty()) {
//            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
//            return "login";
//        }
//
//        User user = userOpt.get();
//
//        // Verify password using BCrypt
//        if (!passwordEncoder.matches(password, user.getPassword())) {
//            model.addAttribute("error", "اسم المستخدم أو كلمة المرور غير صحيحة");
//            return "login";
//        }
//
//        // Login successful - store in session
//        session.setAttribute("userId", user.getId());
//        session.setAttribute("fullName", user.getFullName());
//        session.setAttribute("username", user.getUsername());
//        session.setAttribute("userType", "teacher");
//
//        return "redirect:/dashboard";
//    }
//
//    // ========== LOGOUT ==========
//    @GetMapping("/logout")
//    public String logout(HttpSession session) {
//        session.invalidate();
//        return "redirect:/login?logout=success";
//    }
}