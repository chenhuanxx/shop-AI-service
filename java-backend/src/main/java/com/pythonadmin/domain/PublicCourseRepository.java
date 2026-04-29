package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicCourseRepository extends JpaRepository<PublicCourseEntity, String> {
    List<PublicCourseEntity> findByTabOrderByTimeDesc(String tab);
}
