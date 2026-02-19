package com.academy.controller;

import com.academy.entity.Student;
import com.academy.entity.Video;
import com.academy.repository.StudentRepository;
import com.academy.repository.VideoRepository;
import com.academy.repository.SessionRepository;
import com.academy.repository.PaymentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;




//todo : this not used right now, we need to implement the new addation on the dashbord screen Then use this
@Controller
public class DashboardController {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private VideoRepository videoRepository;

    @Autowired(required = false)
    private SessionRepository sessionRepository;

    @Autowired(required = false)
    private PaymentRepository paymentRepository;

    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        String fullName = (String) session.getAttribute("fullName");

        if (userId == null) {
            return "redirect:/admin/login";
        }

        // Add user info
        model.addAttribute("fullName", fullName);
        model.addAttribute("activePage", "dashboard");
        model.addAttribute("pageTitle", "لوحة التحكم");
        model.addAttribute("breadcrumbs", "لوحة التحكم");

        return "dashboard";
    }

}