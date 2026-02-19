package com.academy.repository;

import com.academy.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTeacherIdOrderByPaymentDateDesc(Long teacherId);

    List<Payment> findByStudentId(Long studentId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.teacherId = :teacherId " +
            "AND MONTH(p.paymentDate) = :month AND YEAR(p.paymentDate) = :year")
    BigDecimal sumByTeacherIdAndMonth(
            @Param("teacherId") Long teacherId,
            @Param("month") int month,
            @Param("year") int year);


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.teacherId = :teacherId AND p.paymentDate BETWEEN :startDate AND :endDate")
    double sumAmountByTeacherIdAndDateRange(Long teacherId, LocalDate startDate, LocalDate endDate);

}