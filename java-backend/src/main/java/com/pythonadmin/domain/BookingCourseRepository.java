package com.pythonadmin.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookingCourseRepository extends JpaRepository<BookingCourseEntity, String> {
    @Query("""
        select c from BookingCourseEntity c
        where (
            :keyword = ''
            or lower(c.name) like lower(:pattern)
            or lower(c.teacherName) like lower(:pattern)
            or lower(c.startAt) like lower(:pattern)
        )
        order by c.startAt asc
        """)
    List<BookingCourseEntity> search(@Param("keyword") String keyword, @Param("pattern") String pattern);
}
