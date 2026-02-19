package com.academy.repository;


import com.academy.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    List<Session> findByTeacherId(Long teacherId);

    List<Session> findByTeacherIdAndSessionDate(Long teacherId, LocalDate date);

    List<Session> findByTeacherIdAndSessionDateBetween(
            Long teacherId, LocalDate startDate, LocalDate endDate);

    List<Session> findByTeacherIdOrderBySessionDateDescSessionTimeDesc(Long teacherId);

    int countByTeacherIdAndSessionDate(Long teacherId, LocalDate date);
}