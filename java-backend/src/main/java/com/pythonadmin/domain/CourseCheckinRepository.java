package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseCheckinRepository extends JpaRepository<CourseCheckinEntity, Long> {
    List<CourseCheckinEntity> findByUserIdAndCourseId(Integer userId, String courseId);
    List<CourseCheckinEntity> findByUserId(Integer userId);
    List<CourseCheckinEntity> findByCourseId(String courseId);
}
