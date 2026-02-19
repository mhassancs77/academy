package com.academy.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

//@Controller
public class HomeController {

//    @GetMapping("/")
//    public String home() {
//        return "index";
//    }

    // ========== NEW: DASHBOARD WITH NAVIGATION ==========
//    @GetMapping("/dashboard")
//    public String dashboard(HttpSession session, Model model) {
//        Long userId = (Long) session.getAttribute("userId");
//        String fullName = (String) session.getAttribute("fullName");
//
//        if (userId == null) {
//            return "redirect:/login";
//        }
//
//        model.addAttribute("fullName", fullName);
//        return "dashboard";
//    }
}