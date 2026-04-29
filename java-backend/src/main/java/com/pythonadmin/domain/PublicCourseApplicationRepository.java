package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCourseApplicationRepository extends JpaRepository<PublicCourseApplicationEntity, String> {
    List<PublicCourseApplicationEntity> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<PublicCourseApplicationEntity> findAllByOrderByCreatedAtDesc();
}

