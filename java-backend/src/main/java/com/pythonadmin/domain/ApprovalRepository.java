package com.pythonadmin.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApprovalRepository extends JpaRepository<ApprovalEntity, String> {
    Optional<ApprovalEntity> findFirstByUserIdAndCourseIdAndTitleOrderByCreatedAtDesc(Integer userId, String courseId, String title);

    List<ApprovalEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<ApprovalEntity> findByUserIdAndTitleAndState(Integer userId, String title, String state);

    List<ApprovalEntity> findByCourseIdAndTitleAndState(String courseId, String title, String state);

    List<ApprovalEntity> findByUserIdAndCourseIdAndTitle(Integer userId, String courseId, String title);

    @Query("SELECT a FROM ApprovalEntity a WHERE a.userId = :userId AND a.courseId = :courseId AND a.title = :title AND a.state = :state")
    List<ApprovalEntity> findByUserIdAndCourseIdAndTitleAndState(
        @Param("userId") Integer userId,
        @Param("courseId") String courseId,
        @Param("title") String title,
        @Param("state") String state
    );

    List<ApprovalEntity> findByStateIn(List<String> states);
}
