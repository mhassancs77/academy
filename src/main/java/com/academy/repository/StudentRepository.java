package com.academy.repository;

import com.academy.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByTeacherId(Long teacherId);
    List<Student> findByTeacherIdOrderByFullNameAsc(Long teacherId);

    // For statistics
    long countByTeacherId(Long teacherId);

    // For recent activities
    List<Student> findTop3ByTeacherIdOrderByCreatedAtDesc(Long teacherId);
}




