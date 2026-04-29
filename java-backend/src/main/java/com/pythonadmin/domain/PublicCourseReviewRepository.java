package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCourseReviewRepository extends JpaRepository<PublicCourseReviewEntity, Long> {
    List<PublicCourseReviewEntity> findByCourseIdOrderByCreatedAtDesc(String courseId);
    List<PublicCourseReviewEntity> findByUserIdAndCourseId(Integer userId, String courseId);
    List<PublicCourseReviewEntity> findByUserId(Integer userId);
}

