package com.attendence.management.repository;
 
import com.attendence.management.domain.Teacher;
import com.attendence.management.domain.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
 
@Repository
public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    @Query("SELECT t FROM Teacher t WHERE t.teacherId= ?1")
    Teacher findByTeacherId(String teacherId);
 

}