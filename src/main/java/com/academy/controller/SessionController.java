package com.academy.controller;

import com.academy.entity.Session;
import com.academy.entity.Student;
import com.academy.repository.SessionRepository;
import com.academy.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class SessionController {

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========== CALENDAR PAGE ==========
    @GetMapping("/calendar")
    public String calendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            HttpSession httpSession,
            Model model) {

        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        // Default to current month
        YearMonth currentMonth = (year != null && month != null)
                ? YearMonth.of(year, month)
                : YearMonth.now();

        // Get sessions for this month
        LocalDate startDate = currentMonth.atDay(1);
        LocalDate endDate = currentMonth.atEndOfMonth();
        List<com.academy.entity.Session> sessions =
                sessionRepository.findByTeacherIdAndSessionDateBetween(userId, startDate, endDate);

        // Get student names
        List<Student> students = studentRepository.findByTeacherId(userId);
        Map<Long, String> studentNames = students.stream()
                .collect(Collectors.toMap(Student::getId, Student::getFullName));

        // Add student names to sessions
        sessions.forEach(s -> s.setStudentName(studentNames.get(s.getStudentId())));

        // Get today's sessions
        List<com.academy.entity.Session> todaySessions =
                sessionRepository.findByTeacherIdAndSessionDate(userId, LocalDate.now());
        todaySessions.forEach(s -> s.setStudentName(studentNames.get(s.getStudentId())));

        model.addAttribute("currentMonth", currentMonth);
        model.addAttribute("sessions", sessions);
        model.addAttribute("todaySessions", todaySessions);
        model.addAttribute("fullName", httpSession.getAttribute("fullName"));

        return "calendar";
    }

    // ========== ADD SESSION PAGE ==========
    @GetMapping("/sessions/add")
    public String addSessionPage(HttpSession httpSession, Model model) {
        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        List<Student> students = studentRepository.findByTeacherId(userId);
        model.addAttribute("students", students);
        model.addAttribute("fullName", httpSession.getAttribute("fullName"));

        return "add-session";
    }

    // ========== ADD SESSION ACTION ==========
    @PostMapping("/sessions/add")
    public String addSession(
            @RequestParam Long studentId,
            @RequestParam String sessionDate,
            @RequestParam String sessionTime,
            @RequestParam Integer durationMinutes,
            @RequestParam(required = false) String notes,
            HttpSession httpSession) {

        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        com.academy.entity.Session session = new com.academy.entity.Session();
        session.setTeacherId(userId);
        session.setStudentId(studentId);
        session.setSessionDate(LocalDate.parse(sessionDate));
        session.setSessionTime(LocalTime.parse(sessionTime));
        session.setDurationMinutes(durationMinutes);
        session.setNotes(notes);

        sessionRepository.save(session);

        return "redirect:/calendar";
    }
}