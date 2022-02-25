package com.attendence.management.repository;
 
import java.sql.Date;
import java.util.List;
import java.util.Set;

import com.attendence.management.domain.Attendence;
import com.attendence.management.domain.Student;
import com.attendence.management.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
 
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {


    @Query("SELECT a FROM Attendence a WHERE a.date= ?1 and a.student = ?2")
    Attendence findByStudentIdandDate(Date date,Student student);

    @Query("SELECT a FROM Student a WHERE a.studentId= ?1")
    Student findByStudentId(String studentId);
 
}