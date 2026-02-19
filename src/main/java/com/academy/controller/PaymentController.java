package com.academy.controller;

import com.academy.entity.Payment;
import com.academy.entity.Student;
import com.academy.repository.PaymentRepository;
import com.academy.repository.StudentRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private StudentRepository studentRepository;

    // ========== PAYMENTS LIST ==========
    @GetMapping("/payments")
    public String listPayments(HttpSession httpSession, Model model) {
        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        // Get all payments
        List<Payment> payments =
                paymentRepository.findByTeacherIdOrderByPaymentDateDesc(userId);

        // Get student names
        List<Student> students = studentRepository.findByTeacherId(userId);
        Map<Long, String> studentNames = students.stream()
                .collect(Collectors.toMap(Student::getId, Student::getFullName));

        // Add student names to payments
        payments.forEach(p -> p.setStudentName(studentNames.get(p.getStudentId())));

        // Calculate monthly total
        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthlyTotal = paymentRepository.sumByTeacherIdAndMonth(
                userId, currentMonth.getMonthValue(), currentMonth.getYear());

        if (monthlyTotal == null) {
            monthlyTotal = BigDecimal.ZERO;
        }

        model.addAttribute("payments", payments);
        model.addAttribute("monthlyTotal", monthlyTotal);
        model.addAttribute("fullName", httpSession.getAttribute("fullName"));

        return "payments-list";
    }

    // ========== ADD PAYMENT PAGE ==========
    @GetMapping("/payments/add")
    public String addPaymentPage(HttpSession httpSession, Model model) {
        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        List<Student> students = studentRepository.findByTeacherId(userId);
        model.addAttribute("students", students);
        model.addAttribute("fullName", httpSession.getAttribute("fullName"));

        return "add-payment";
    }

    // ========== ADD PAYMENT ACTION ==========
    @PostMapping("/payments/add")
    public String addPayment(
            @RequestParam Long studentId,
            @RequestParam BigDecimal amount,
            @RequestParam String paymentDate,
            @RequestParam(required = false) String notes,
            HttpSession httpSession) {

        Long userId = (Long) httpSession.getAttribute("userId");
        if (userId == null) {
            return "redirect:/admin/login";
        }

        Payment payment = new Payment();
        payment.setTeacherId(userId);
        payment.setStudentId(studentId);
        payment.setAmount(amount);
        payment.setPaymentDate(LocalDate.parse(paymentDate));
        payment.setNotes(notes);

        paymentRepository.save(payment);

        return "redirect:/payments";
    }
}
